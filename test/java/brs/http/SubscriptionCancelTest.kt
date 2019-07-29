package brs.http

import brs.*
import brs.common.QuickMocker
import brs.common.QuickMocker.MockParam
import brs.fluxcapacitor.FluxCapacitor
import brs.fluxcapacitor.FluxValues
import brs.services.ParameterService
import brs.services.SubscriptionService
import brs.util.JSON
import com.google.gson.JsonObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import javax.servlet.http.HttpServletRequest

import brs.TransactionType.AdvancedPayment.SUBSCRIPTION_CANCEL
import brs.http.common.Parameters.SUBSCRIPTION_PARAMETER
import brs.http.common.ResultFields.ERROR_CODE_RESPONSE
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SubscriptionCancelTest : AbstractTransactionTest() {

    private var t: SubscriptionCancel? = null

    private var parameterServiceMock: ParameterService? = null
    private var subscriptionServiceMock: SubscriptionService? = null
    private var blockchainMock: Blockchain? = null
    private var apiTransactionManagerMock: APITransactionManager? = null

    @Before
    fun setUp() {
        parameterServiceMock = mock<ParameterService>()
        subscriptionServiceMock = mock<SubscriptionService>()
        blockchainMock = mock<Blockchain>()
        apiTransactionManagerMock = mock<APITransactionManager>()

        t = SubscriptionCancel(parameterServiceMock!!, subscriptionServiceMock!!, blockchainMock!!, apiTransactionManagerMock!!)
    }

    @Test
    @Throws(BurstException::class)
    fun processRequest() {
        val subscriptionIdParameter = 123L

        val req = QuickMocker.httpServletRequest(
                MockParam(SUBSCRIPTION_PARAMETER, subscriptionIdParameter)
        )

        val mockSender = mock<Account>()
        whenever(mockSender.getId()).doReturn(1L)

        val mockSubscription = mock<Subscription>()
        whenever(mockSubscription.getId()).doReturn(subscriptionIdParameter)
        whenever(mockSubscription.getSenderId()).doReturn(1L)
        whenever(mockSubscription.getRecipientId()).doReturn(2L)

        whenever(parameterServiceMock!!.getSenderAccount(eq<HttpServletRequest>(req))).doReturn(mockSender)
        whenever(subscriptionServiceMock!!.getSubscription(eq<Long>(subscriptionIdParameter))).doReturn(mockSubscription)

        QuickMocker.fluxCapacitorEnabledFunctionalities(FluxValues.DIGITAL_GOODS_STORE)

        val attachment = attachmentCreatedTransaction({ t!!.processRequest(req) }, apiTransactionManagerMock!!) as Attachment.AdvancedPaymentSubscriptionCancel
        assertNotNull(attachment)

        assertEquals(SUBSCRIPTION_CANCEL, attachment.transactionType)
        assertEquals(subscriptionIdParameter, attachment.subscriptionId)
    }

    @Test
    @Throws(BurstException::class)
    fun processRequest_missingSubscriptionParameter() {
        val req = QuickMocker.httpServletRequest()

        val response = t!!.processRequest(req) as JsonObject
        assertNotNull(response)

        assertEquals(3, JSON.getAsInt(response.get(ERROR_CODE_RESPONSE)).toLong())
    }

    @Test
    @Throws(BurstException::class)
    fun processRequest_failedToParseSubscription() {
        val req = QuickMocker.httpServletRequest(
                MockParam(SUBSCRIPTION_PARAMETER, "notALong")
        )

        val response = t!!.processRequest(req) as JsonObject
        assertNotNull(response)

        assertEquals(4, JSON.getAsInt(response.get(ERROR_CODE_RESPONSE)).toLong())
    }

    @Test
    @Throws(BurstException::class)
    fun processRequest_subscriptionNotFound() {
        val subscriptionId = 123L

        val req = QuickMocker.httpServletRequest(
                MockParam(SUBSCRIPTION_PARAMETER, subscriptionId)
        )

        whenever(subscriptionServiceMock!!.getSubscription(eq(subscriptionId))).doReturn(null)

        val response = t!!.processRequest(req) as JsonObject
        assertNotNull(response)

        assertEquals(5, JSON.getAsInt(response.get(ERROR_CODE_RESPONSE)).toLong())
    }

    @Test
    @Throws(BurstException::class)
    fun processRequest_userIsNotSenderOrRecipient() {
        val subscriptionId = 123L

        val req = QuickMocker.httpServletRequest(
                MockParam(SUBSCRIPTION_PARAMETER, subscriptionId)
        )

        val mockSender = mock<Account>()
        whenever(mockSender.getId()).doReturn(1L)

        val mockSubscription = mock<Subscription>()
        whenever(mockSubscription.getSenderId()).doReturn(2L)
        whenever(mockSubscription.getRecipientId()).doReturn(3L)

        whenever(parameterServiceMock!!.getSenderAccount(eq<HttpServletRequest>(req))).doReturn(mockSender)
        whenever(subscriptionServiceMock!!.getSubscription(eq(subscriptionId))).doReturn(mockSubscription)

        val response = t!!.processRequest(req) as JsonObject
        assertNotNull(response)

        assertEquals(7, JSON.getAsInt(response.get(ERROR_CODE_RESPONSE)).toLong())
    }
}
