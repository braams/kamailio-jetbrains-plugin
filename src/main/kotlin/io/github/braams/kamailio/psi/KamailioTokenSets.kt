package io.github.braams.kamailio.psi

import com.intellij.psi.tree.TokenSet

object KamailioTokenSets {
    @JvmField
    val COMMENTS: TokenSet = TokenSet.create(KamailioTypes.LINE_COMMENT, KamailioTypes.BLOCK_COMMENT)

    @JvmField
    val STRINGS: TokenSet = TokenSet.create(
        KamailioTypes.DQUOTE, KamailioTypes.STRING_TEXT, KamailioTypes.STRING_INTERP, KamailioTypes.SQ_STRING
    )

    @JvmField
    val KEYWORDS: TokenSet = TokenSet.create(
        KamailioTypes.IF_KW, KamailioTypes.ELSE_KW, KamailioTypes.SWITCH_KW, KamailioTypes.CASE_KW,
        KamailioTypes.DEFAULT_KW, KamailioTypes.WHILE_KW, KamailioTypes.BREAK_KW, KamailioTypes.RETURN_KW,
        KamailioTypes.EXIT_KW, KamailioTypes.DROP_KW,
        KamailioTypes.ROUTE_KW, KamailioTypes.REQUEST_ROUTE_KW, KamailioTypes.REPLY_ROUTE_KW,
        KamailioTypes.ONREPLY_ROUTE_KW, KamailioTypes.FAILURE_ROUTE_KW, KamailioTypes.BRANCH_ROUTE_KW,
        KamailioTypes.ONSEND_ROUTE_KW, KamailioTypes.EVENT_ROUTE_KW,
        KamailioTypes.LOADMODULE_KW, KamailioTypes.LOADPATH_KW, KamailioTypes.MODPARAM_KW,
        KamailioTypes.INCLUDE_FILE_KW, KamailioTypes.IMPORT_FILE_KW
    )

    @JvmField
    val ROUTE_KIND_KEYWORDS: TokenSet = TokenSet.create(
        KamailioTypes.ROUTE_KW, KamailioTypes.REQUEST_ROUTE_KW, KamailioTypes.REPLY_ROUTE_KW,
        KamailioTypes.ONREPLY_ROUTE_KW, KamailioTypes.FAILURE_ROUTE_KW, KamailioTypes.BRANCH_ROUTE_KW,
        KamailioTypes.ONSEND_ROUTE_KW, KamailioTypes.EVENT_ROUTE_KW
    )

    @JvmField
    val PP_DIRECTIVES: TokenSet = TokenSet.create(
        KamailioTypes.PP_SHEBANG_KW, KamailioTypes.PP_DEFINE_KW, KamailioTypes.PP_UNDEF_KW,
        KamailioTypes.PP_IFDEF_KW, KamailioTypes.PP_IFNDEF_KW, KamailioTypes.PP_IFEXP_KW,
        KamailioTypes.PP_ELSE_KW, KamailioTypes.PP_ENDIF_KW, KamailioTypes.PP_SUBST_KW,
        KamailioTypes.PP_DEFENV_KW, KamailioTypes.PP_UNKNOWN_KW
    )

    @JvmField
    val OPERATORS: TokenSet = TokenSet.create(
        KamailioTypes.EQEQ, KamailioTypes.NEQ, KamailioTypes.MATCH_OP, KamailioTypes.NMATCH_OP,
        KamailioTypes.LE, KamailioTypes.GE, KamailioTypes.LT, KamailioTypes.GT,
        KamailioTypes.ANDAND, KamailioTypes.OROR, KamailioTypes.FATARROW, KamailioTypes.EQ,
        KamailioTypes.NOT, KamailioTypes.PLUS, KamailioTypes.MINUS, KamailioTypes.STAR,
        KamailioTypes.SLASH, KamailioTypes.AMP, KamailioTypes.PIPE
    )
}
