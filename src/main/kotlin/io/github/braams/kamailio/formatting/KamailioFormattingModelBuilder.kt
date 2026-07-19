package io.github.braams.kamailio.formatting

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet
import io.github.braams.kamailio.KamailioLanguage
import io.github.braams.kamailio.psi.KamailioTypes

class KamailioFormattingModelBuilder : FormattingModelBuilder {

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val rootBlock = KamailioBlock(formattingContext.node, null, null, createSpacingBuilder(settings))
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile, rootBlock, settings
        )
    }

    /**
     * Spacing follows the conventions of the stock kamailio.cfg by default (comparisons and global
     * assignments tight, logical operators and route-level assignments spaced, `if (`), each rule
     * wired to the standard IntelliJ spacing checkbox shown on the Kamailio code style page.
     *
     * Rules are deliberately scoped with aroundInside/betweenInside: nothing may touch the inside
     * of pseudo-variables (`$var(x)`, `$sht(t=>$k)`, transformations) — whitespace there can change
     * the runtime meaning of the key.
     */
    private fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
        val common = settings.getCommonSettings(KamailioLanguage)
        return SpacingBuilder(settings, KamailioLanguage)
            // ---- commas and semicolons ----
            // scoped to call arguments and modparam only: commas inside pseudo-variable keys and
            // transformations ($(rU{s.substr,0,4})) must stay exactly as typed
            .beforeInside(KamailioTypes.COMMA, KamailioTypes.ARG_LIST).spaceIf(common.SPACE_BEFORE_COMMA)
            .afterInside(KamailioTypes.COMMA, KamailioTypes.ARG_LIST).spaceIf(common.SPACE_AFTER_COMMA)
            .beforeInside(KamailioTypes.COMMA, KamailioTypes.MODPARAM_STMT).spaceIf(common.SPACE_BEFORE_COMMA)
            .afterInside(KamailioTypes.COMMA, KamailioTypes.MODPARAM_STMT).spaceIf(common.SPACE_AFTER_COMMA)
            .before(KamailioTypes.SEMICOLON).spaces(0)

            // ---- unary operators stay tight (must precede the binary-operator rules) ----
            .afterInside(KamailioTypes.NOT, KamailioTypes.UNARY_EXPR).spaces(0)
            .afterInside(KamailioTypes.MINUS, KamailioTypes.UNARY_EXPR).spaces(0)
            .afterInside(KamailioTypes.MINUS, KamailioTypes.CASE_VALUE).spaces(0)
            .afterInside(KamailioTypes.MINUS, KamailioTypes.MODPARAM_VALUE).spaces(0)

            // ---- assignments: spaced inside routes, tight for global params (stock style) ----
            .aroundInside(KamailioTypes.EQ, KamailioTypes.ASSIGN_STMT)
            .spaceIf(common.SPACE_AROUND_ASSIGNMENT_OPERATORS)
            .aroundInside(KamailioTypes.EQ, KamailioTypes.GLOBAL_ASSIGNMENT).spaces(0)

            // ---- binary operators ----
            .around(TokenSet.create(KamailioTypes.EQEQ, KamailioTypes.NEQ,
                                    KamailioTypes.MATCH_OP, KamailioTypes.NMATCH_OP))
            .spaceIf(common.SPACE_AROUND_EQUALITY_OPERATORS)
            .around(TokenSet.create(KamailioTypes.LT, KamailioTypes.GT, KamailioTypes.LE, KamailioTypes.GE))
            .spaceIf(common.SPACE_AROUND_RELATIONAL_OPERATORS)
            .around(TokenSet.create(KamailioTypes.ANDAND, KamailioTypes.OROR))
            .spaceIf(common.SPACE_AROUND_LOGICAL_OPERATORS)
            .aroundInside(KamailioTypes.PLUS, KamailioTypes.ADD_EXPR)
            .spaceIf(common.SPACE_AROUND_ADDITIVE_OPERATORS)
            .aroundInside(KamailioTypes.MINUS, KamailioTypes.ADD_EXPR)
            .spaceIf(common.SPACE_AROUND_ADDITIVE_OPERATORS)

            // ---- keyword parentheses: if ( / switch ( / while ( ----
            .betweenInside(KamailioTypes.IF_KW, KamailioTypes.CONDITION, KamailioTypes.IF_STMT)
            .spaceIf(common.SPACE_BEFORE_IF_PARENTHESES)
            .betweenInside(KamailioTypes.SWITCH_KW, KamailioTypes.CONDITION, KamailioTypes.SWITCH_STMT)
            .spaceIf(common.SPACE_BEFORE_SWITCH_PARENTHESES)
            .betweenInside(KamailioTypes.WHILE_KW, KamailioTypes.CONDITION, KamailioTypes.WHILE_STMT)
            .spaceIf(common.SPACE_BEFORE_WHILE_PARENTHESES)

            // ---- call parentheses: t_relay( / route( / modparam( ----
            .betweenInside(KamailioTypes.IDENTIFIER, KamailioTypes.LPAREN, KamailioTypes.CALL_EXPR)
            .spaceIf(common.SPACE_BEFORE_METHOD_CALL_PARENTHESES)
            .betweenInside(KamailioTypes.ROUTE_KW, KamailioTypes.LPAREN, KamailioTypes.ROUTE_CALL)
            .spaceIf(common.SPACE_BEFORE_METHOD_CALL_PARENTHESES)
            .betweenInside(KamailioTypes.MODPARAM_KW, KamailioTypes.LPAREN, KamailioTypes.MODPARAM_STMT)
            .spaceIf(common.SPACE_BEFORE_METHOD_CALL_PARENTHESES)

            // ---- within parentheses (conditions, calls, grouping — NOT pseudo-variables) ----
            .afterInside(KamailioTypes.LPAREN, KamailioTypes.CONDITION).spaceIf(common.SPACE_WITHIN_PARENTHESES)
            .beforeInside(KamailioTypes.RPAREN, KamailioTypes.CONDITION).spaceIf(common.SPACE_WITHIN_PARENTHESES)
            .afterInside(KamailioTypes.LPAREN, KamailioTypes.PAREN_EXPR).spaceIf(common.SPACE_WITHIN_PARENTHESES)
            .beforeInside(KamailioTypes.RPAREN, KamailioTypes.PAREN_EXPR).spaceIf(common.SPACE_WITHIN_PARENTHESES)
            .afterInside(KamailioTypes.LPAREN, KamailioTypes.CALL_EXPR).spaceIf(common.SPACE_WITHIN_PARENTHESES)
            .beforeInside(KamailioTypes.RPAREN, KamailioTypes.CALL_EXPR).spaceIf(common.SPACE_WITHIN_PARENTHESES)
            .afterInside(KamailioTypes.LPAREN, KamailioTypes.ROUTE_CALL).spaceIf(common.SPACE_WITHIN_PARENTHESES)
            .beforeInside(KamailioTypes.RPAREN, KamailioTypes.ROUTE_CALL).spaceIf(common.SPACE_WITHIN_PARENTHESES)
            .afterInside(KamailioTypes.LPAREN, KamailioTypes.MODPARAM_STMT).spaceIf(common.SPACE_WITHIN_PARENTHESES)
            .beforeInside(KamailioTypes.RPAREN, KamailioTypes.MODPARAM_STMT).spaceIf(common.SPACE_WITHIN_PARENTHESES)

            // ---- braces and else ----
            .betweenInside(KamailioTypes.CONDITION, KamailioTypes.STATEMENT, KamailioTypes.IF_STMT).spaces(1)
            .betweenInside(KamailioTypes.CONDITION, KamailioTypes.STATEMENT, KamailioTypes.WHILE_STMT).spaces(1)
            .betweenInside(KamailioTypes.CONDITION, KamailioTypes.LBRACE, KamailioTypes.SWITCH_STMT).spaces(1)
            .between(KamailioTypes.ROUTE_NAME_DECL, KamailioTypes.BLOCK).spaces(1)
            .between(KamailioTypes.ROUTE_KIND, KamailioTypes.BLOCK).spaces(1)
            .beforeInside(KamailioTypes.ELSE_CLAUSE, KamailioTypes.IF_STMT)
            .spaceIf(common.SPACE_BEFORE_ELSE_KEYWORD)
            .afterInside(KamailioTypes.ELSE_KW, KamailioTypes.ELSE_CLAUSE).spaces(1)

            // ---- case labels and top-level keyword statements ----
            .beforeInside(KamailioTypes.COLON, KamailioTypes.CASE_CLAUSE).spaces(0)
            .beforeInside(KamailioTypes.COLON, KamailioTypes.DEFAULT_CLAUSE).spaces(0)
            .after(KamailioTypes.CASE_KW).spaces(1)
            .after(KamailioTypes.LOADMODULE_KW).spaces(1)
            .after(KamailioTypes.LOADPATH_KW).spaces(1)
            .after(KamailioTypes.INCLUDE_FILE_KW).spaces(1)
            .after(KamailioTypes.IMPORT_FILE_KW).spaces(1)
    }
}
