package brs.http

import brs.BurstException
import brs.assetexchange.AssetExchange
import brs.http.common.Parameters.ASSET_PARAMETER
import brs.http.common.Parameters.FIRST_INDEX_PARAMETER
import brs.http.common.Parameters.LAST_INDEX_PARAMETER
import brs.services.ParameterService
import brs.util.toUnsignedString
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import javax.servlet.http.HttpServletRequest

internal class GetBidOrderIds(private val parameterService: ParameterService, private val assetExchange: AssetExchange) : APIServlet.JsonRequestHandler(arrayOf(APITag.AE), ASSET_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER) {

    @Throws(BurstException::class)
    internal override fun processRequest(request: HttpServletRequest): JsonElement {
        val assetId = parameterService.getAsset(request).id
        val firstIndex = ParameterParser.getFirstIndex(request)
        val lastIndex = ParameterParser.getLastIndex(request)

        val orderIds = JsonArray()
        for (bidOrder in assetExchange.getSortedBidOrders(assetId, firstIndex, lastIndex)) {
            orderIds.add(bidOrder.id.toUnsignedString())
        }
        val response = JsonObject()
        response.add("bidOrderIds", orderIds)
        return response
    }

}
