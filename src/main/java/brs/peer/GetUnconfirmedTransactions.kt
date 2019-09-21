package brs.peer

import brs.Transaction
import brs.TransactionProcessor
import brs.peer.PeerServlet.ExtendedProcessRequest
import com.google.gson.JsonArray
import com.google.gson.JsonObject

import brs.http.common.ResultFields.UNCONFIRMED_TRANSACTIONS_RESPONSE

internal class GetUnconfirmedTransactions(private val transactionProcessor: TransactionProcessor) : PeerServlet.ExtendedPeerRequestHandler() {
    internal override fun extendedProcessRequest(request: JsonObject, peer: Peer): ExtendedProcessRequest {
        val response = JsonObject()

        val unconfirmedTransactions = transactionProcessor.getAllUnconfirmedTransactionsFor(peer)

        val transactionsData = JsonArray()
        for (transaction in unconfirmedTransactions) {
            transactionsData.add(transaction.jsonObject)
        }

        response.add(UNCONFIRMED_TRANSACTIONS_RESPONSE, transactionsData)

        return ExtendedProcessRequest(response) { transactionProcessor.markFingerPrintsOf(peer, unconfirmedTransactions) }
    }
}
