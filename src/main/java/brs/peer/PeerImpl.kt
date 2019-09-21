package brs.peer

import brs.*
import brs.crypto.Crypto
import brs.props.Props
import brs.util.*
import brs.util.delegates.Atomic
import brs.util.delegates.AtomicWithOverride
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import java.io.*
import java.net.*
import java.nio.charset.StandardCharsets
import java.sql.SQLException
import java.util.*
import java.util.zip.GZIPInputStream

internal class PeerImpl(private val dp: DependencyProvider, override val peerAddress: String, announcedAddress: String?) : Peer {
    override var announcedAddress by AtomicWithOverride<String?>(setValueDelegate = { announcedAddress, set ->
        val announcedPeerAddress = Peers.normalizeHostAndPort(announcedAddress)
        if (announcedPeerAddress != null) {
            set(announcedPeerAddress)
            try {
                this.port = URL(Constants.HTTP + announcedPeerAddress).port
            } catch (ignored: MalformedURLException) {
            }
        }
    })
    override var port by Atomic<Int>()
    override var shareAddress by Atomic(false)
    override var platform by Atomic<String>()
    override var application by Atomic<String>()
    override var version by Atomic<Version>()
    private var isOldVersion by Atomic(false)
    private var blacklistingTime by Atomic<Long>()
    override var state: Peer.State by AtomicWithOverride(setValueDelegate = { newState, set ->
        if (state != newState) {
            if (state == Peer.State.NON_CONNECTED) {
                set(newState)
                Peers.notifyListeners(this, Peers.Event.ADDED_ACTIVE_PEER)
            } else if (newState != Peer.State.NON_CONNECTED) {
                set(newState)
                Peers.notifyListeners(this, Peers.Event.CHANGED_ACTIVE_PEER)
            }
        }
    })
    override var downloadedVolume by Atomic<Long>()
    override var uploadedVolume by Atomic<Long>()
    override var lastUpdated by Atomic<Int>()
    private var lastDownloadedTransactionsDigest: ByteArray? = null
    private val lastDownloadedTransactionsLock = Any()

    override val isAtLeastMyVersion: Boolean
        get() = isHigherOrEqualVersionThan(Burst.VERSION)

    override val software: String
        get() = (Convert.truncate(application, "?", 10, false)
                + " (" + Convert.truncate(version.toString(), "?", 10, false) + ")"
                + " @ " + Convert.truncate(platform, "?", 10, false))

    override val isWellKnown: Boolean
        get() = announcedAddress != null && Peers.wellKnownPeers.contains(announcedAddress!!)

    override val isRebroadcastTarget: Boolean
        get() = announcedAddress != null && Peers.rebroadcastPeers.contains(announcedAddress!!)

    override val isBlacklisted: Boolean
        get() = blacklistingTime > 0 || isOldVersion || Peers.knownBlacklistedPeers.contains(peerAddress)

    init {
        this.announcedAddress = announcedAddress
        try {
            this.port = URL(Constants.HTTP + announcedAddress).port
        } catch (ignored: MalformedURLException) {
        }

        this.state = Peer.State.NON_CONNECTED
        this.version = Version.EMPTY //not null
        this.shareAddress = true
    }

    // TODO unused
    fun diffLastDownloadedTransactions(data: ByteArray): Boolean {
        synchronized(lastDownloadedTransactionsLock) {
            val newDigest = Crypto.sha256().digest(data)
            if (lastDownloadedTransactionsDigest != null && Arrays.equals(newDigest, lastDownloadedTransactionsDigest)) {
                return false
            }
            lastDownloadedTransactionsDigest = newDigest
            return true
        }
    }

    override fun updateDownloadedVolume(volume: Long) {
        synchronized(this) {
            downloadedVolume += volume
        }
        Peers.notifyListeners(this, Peers.Event.DOWNLOADED_VOLUME)
    }

    override fun updateUploadedVolume(volume: Long) {
        synchronized(this) {
            uploadedVolume += volume
        }
        Peers.notifyListeners(this, Peers.Event.UPLOADED_VOLUME)
    }

    override fun isHigherOrEqualVersionThan(version: Version): Boolean {
        return Peer.isHigherOrEqualVersion(version, this.version)
    }

    override fun setVersion(version: String?) {
        this.version = Version.EMPTY
        isOldVersion = false
        if (Burst.APPLICATION == application && version != null) {
            try {
                this.version = Version.parse(version)
                isOldVersion = Constants.MIN_VERSION.isGreaterThan(this.version)
            } catch (e: IllegalArgumentException) {
                isOldVersion = true
            }
        }
    }

    override fun blacklist(cause: Exception, description: String) {
        if (cause is BurstException.NotCurrentlyValidException || cause is BlockchainProcessor.BlockOutOfOrderException
                || cause is SQLException || cause.cause is SQLException) {
            // don't blacklist peers just because a feature is not yet enabled, or because of database timeouts
            // prevents erroneous blacklisting during loading of blockchain from scratch
            return
        }
        if (cause is IOException) {
            // don't trigger verbose logging, if we had an IO Exception (eg. network stuff)
            blacklist()
        } else {
            val alreadyBlacklisted = isBlacklisted
            logger.error("Reason for following blacklist: " + cause.message, cause)
            blacklist(description) // refresh blacklist expiry
            if (!alreadyBlacklisted) {
                logger.debug("... because of: {}", cause, cause)
            }
        }
    }

    override fun blacklist(description: String) {
        if (!isBlacklisted && logger.isInfoEnabled) {
            logger.info("Blacklisting {} ({}) because of: {}", peerAddress, version, description)
        }
        blacklist()
    }

    override fun blacklist() {
        blacklistingTime = System.currentTimeMillis()
        state = Peer.State.NON_CONNECTED
        Peers.notifyListeners(this, Peers.Event.BLACKLIST)
    }

    override fun unBlacklist() {
        state = Peer.State.NON_CONNECTED
        blacklistingTime = 0
        Peers.notifyListeners(this, Peers.Event.UNBLACKLIST)
    }

    override fun updateBlacklistedStatus(curTime: Long) {
        if (blacklistingTime > 0 && blacklistingTime + Peers.blacklistingPeriod <= curTime) {
            unBlacklist()
        }
    }

    override fun remove() {
        Peers.removePeer(this)
        Peers.notifyListeners(this, Peers.Event.REMOVE)
    }

    override fun send(request: JsonElement): JsonObject? {
        var response: JsonObject? = null
        var log: String? = null
        var showLog = false
        var connection: HttpURLConnection? = null

        try {
            val address = if (announcedAddress != null) announcedAddress else peerAddress
            val buf = StringBuilder(Constants.HTTP)
            buf.append(address)
            if (port <= 0) {
                buf.append(':')
                buf.append(if (dp.propertyService.get(Props.DEV_TESTNET)) Peers.TESTNET_PEER_PORT else Peers.DEFAULT_PEER_PORT)
            }
            buf.append("/burst")
            val url = URL(buf.toString())

            if (Peers.communicationLoggingMask != 0) {
                val stringWriter = StringWriter()
                request.writeTo(stringWriter)
                log = "\"$url\": $stringWriter"
            }

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = Peers.connectTimeout
            connection.readTimeout = Peers.readTimeout
            connection.addRequestProperty("User-Agent", "BRS/" + Burst.VERSION.toString())
            connection.setRequestProperty("Accept-Encoding", "gzip")
            connection.setRequestProperty("Connection", "close")

            val cos = CountingOutputStream(connection.outputStream)
            BufferedWriter(OutputStreamWriter(cos, StandardCharsets.UTF_8)).use { writer -> request.writeTo(writer) } // rico666: no catch?
            updateUploadedVolume(cos.count)

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val cis = CountingInputStream(connection.inputStream)
                var responseStream: InputStream = cis
                if ("gzip" == connection.getHeaderField("Content-Encoding")) {
                    responseStream = GZIPInputStream(cis)
                }
                if (Peers.communicationLoggingMask and Peers.LOGGING_MASK_200_RESPONSES != 0) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    responseStream.use { inputStream -> inputStream.copyTo(byteArrayOutputStream, 1024) }
                    val responseValue = byteArrayOutputStream.toString("UTF-8")
                    if (responseValue.isNotEmpty() && responseStream is GZIPInputStream) {
                        log += String.format("[length: %d, compression ratio: %.2f]", cis.count, cis.count.toDouble() / responseValue.length.toDouble())
                    }
                    log += " >>> $responseValue"
                    showLog = true
                    response = JSON.getAsJsonObject(responseValue.parseJson())
                } else {
                    BufferedReader(InputStreamReader(responseStream, StandardCharsets.UTF_8)).use { reader -> response = JSON.getAsJsonObject(reader.parseJson()) }
                }
                updateDownloadedVolume(cis.count)
            } else {
                if (Peers.communicationLoggingMask and Peers.LOGGING_MASK_NON200_RESPONSES != 0) {
                    log += " >>> Peer responded with HTTP " + connection.responseCode + " code!"
                    showLog = true
                }
                state = if (state == Peer.State.CONNECTED) {
                    Peer.State.DISCONNECTED
                } else {
                    Peer.State.NON_CONNECTED
                }
                response = null
            }
        } catch (e: RuntimeException) {
            if (!isConnectionException(e)) {
                logger.debug("Error sending JSON request", e)
            }
            if (Peers.communicationLoggingMask and Peers.LOGGING_MASK_EXCEPTIONS != 0) {
                log += " >>> $e"
                showLog = true
            }
            if (state == Peer.State.CONNECTED) {
                state = Peer.State.DISCONNECTED
            }
            response = null
        } catch (e: IOException) {
            if (!isConnectionException(e)) {
                logger.debug("Error sending JSON request", e)
            }
            if (Peers.communicationLoggingMask and Peers.LOGGING_MASK_EXCEPTIONS != 0) {
                log += " >>> $e"
                showLog = true
            }
            if (state == Peer.State.CONNECTED) {
                state = Peer.State.DISCONNECTED
            }
            response = null
        }

        if (showLog) {
            logger.info(log)
        }

        connection?.disconnect()

        return response
    }

    private fun isConnectionException(e: Throwable): Boolean {
        if (e is UnknownHostException || e is SocketTimeoutException || e is SocketException) return true
        val cause = e.cause
        return cause != null && isConnectionException(cause)
    }

    override fun compareTo(other: Peer): Int {
        return 0
    }

    override fun connect(currentTime: Int) {
        val response = send(Peers.myPeerInfoRequest)
        if (response != null) {
            application = JSON.getAsString(response.get("application"))
            setVersion(JSON.getAsString(response.get("version")))
            platform = JSON.getAsString(response.get("platform"))
            shareAddress = JSON.getAsBoolean(response.get("shareAddress")) == true
            val newAnnouncedAddress = Convert.emptyToNull(JSON.getAsString(response.get("announcedAddress")))
            if (newAnnouncedAddress != null && newAnnouncedAddress != announcedAddress) {
                // force verification of changed announced address
                state = Peer.State.NON_CONNECTED
                announcedAddress = newAnnouncedAddress
                return
            }
            if (announcedAddress == null) {
                announcedAddress = peerAddress
            }

            state = Peer.State.CONNECTED
            Peers.updateAddress(this)
            lastUpdated = currentTime
        } else {
            state = Peer.State.NON_CONNECTED
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PeerImpl::class.java)
    }
}
