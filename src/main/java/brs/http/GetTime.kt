package brs.http

import brs.http.common.ResultFields.TIME_RESPONSE
import brs.services.TimeService
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import javax.servlet.http.HttpServletRequest

internal class GetTime internal constructor(private val timeService: TimeService) : APIServlet.JsonRequestHandler(arrayOf(APITag.INFO)) {

    internal override fun processRequest(request: HttpServletRequest): JsonElement {
        val response = JsonObject()
        response.addProperty(TIME_RESPONSE, timeService.epochTime)

        return response
    }

}
