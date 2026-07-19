package io.github.braams.kamailio.psi

import com.intellij.psi.tree.IElementType

enum class RouteKind(val keyword: String) {
    ROUTE("route"),
    REQUEST("request_route"),
    REPLY("reply_route"),
    ONREPLY("onreply_route"),
    FAILURE("failure_route"),
    BRANCH("branch_route"),
    ONSEND("onsend_route"),
    EVENT("event_route");

    companion object {
        fun fromToken(type: IElementType?): RouteKind? = when (type) {
            KamailioTypes.ROUTE_KW -> ROUTE
            KamailioTypes.REQUEST_ROUTE_KW -> REQUEST
            KamailioTypes.REPLY_ROUTE_KW -> REPLY
            KamailioTypes.ONREPLY_ROUTE_KW -> ONREPLY
            KamailioTypes.FAILURE_ROUTE_KW -> FAILURE
            KamailioTypes.BRANCH_ROUTE_KW -> BRANCH
            KamailioTypes.ONSEND_ROUTE_KW -> ONSEND
            KamailioTypes.EVENT_ROUTE_KW -> EVENT
            else -> null
        }
    }
}
