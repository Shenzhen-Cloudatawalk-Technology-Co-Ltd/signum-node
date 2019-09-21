package brs.unconfirmedtransactions

import brs.BurstException
import brs.Transaction
import brs.peer.Peer

interface UnconfirmedTransactionStore {
    val all: List<Transaction>

    val amount: Int

    @Throws(BurstException.ValidationException::class)
    fun put(transaction: Transaction, peer: Peer?): Boolean

    operator fun get(transactionId: Long?): Transaction?

    fun exists(transactionId: Long?): Boolean

    fun getAllFor(peer: Peer): Collection<Transaction>

    fun remove(transaction: Transaction)

    fun clear()

    /**
     * Review which transactions are still eligible to stay
     * @return The list of removed transactions
     * TODO rename method
     */
    fun resetAccountBalances()

    fun markFingerPrintsOf(peer: Peer?, transactions: Collection<Transaction>)

    fun removeForgedTransactions(transactions: Collection<Transaction>)
}
