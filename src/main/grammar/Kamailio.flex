package io.github.braams.kamailio.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static io.github.braams.kamailio.psi.KamailioTypes.*;

%%

%{
  public _KamailioLexer() {
    this((java.io.Reader)null);
  }

  private final java.util.ArrayDeque<Integer> stateStack = new java.util.ArrayDeque<>();

  private void pushState(int state) {
    stateStack.push(yystate());
    yybegin(state);
  }

  private void popState() {
    yybegin(stateStack.isEmpty() ? YYINITIAL : stateStack.pop());
  }

  public void clearStates() {
    stateStack.clear();
  }
%}

%public
%class _KamailioLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

WS           = [ \t]
CRLF         = \r\n | \n | \r

// Preprocessor directives may start with "#!" or "!!" (see kamailio src/core/cfg.lex PREP_START)
PP           = "#!" | "!!"

IDENT        = [a-zA-Z_][a-zA-Z0-9_]*
// event_route[xhttp:request], event_route[core:worker-one-init] — colons and hyphens allowed
EVENT_NAME_RE = [a-zA-Z][0-9a-zA-Z_-]* (":" [a-zA-Z][0-9a-zA-Z_-]*)*

IP4          = [0-9]{1,3} ("." [0-9]{1,3}){3}
NUMBER_RE    = 0[xX][0-9a-fA-F]+ | [01]+b | [0-9]+

// Pseudo-variable embedded in a double-quoted string: $ru, $var(x), $hdr(CSeq), $(ru{s.len})
STR_PV       = \$ ( [a-zA-Z_][a-zA-Z0-9_]* ( \( [^\)\"\r\n]* \) )? | \( [^\)\"\r\n]* \) )

SQ_STRING_FULL   = ' ( [^'\\\r\n] | \\[^\r\n] )* '
SQ_STRING_OPEN   = ' ( [^'\\\r\n] | \\[^\r\n] )*

BLOCK_COMMENT_FULL = "/*" ( [^*] | \*+[^*/] )* \*+ "/"
BLOCK_COMMENT_OPEN = "/*" ( [^*] | \*+[^*/] )* \**

%x PP_LINE
%x PP_RAW
%x IN_DQ
%x EVRT

%%

// ---------- Preprocessor directive heads (only from normal code) ----------
<YYINITIAL> {
  {PP} ("KAMAILIO"|"OPENSER"|"SER"|"MAXCOMPAT"|"ALL")            { pushState(PP_RAW);  return PP_SHEBANG_KW; }
  {PP} ("define"|"def"|"trydefine"|"trydef"|"redefine"|"redef")  { pushState(PP_LINE); return PP_DEFINE_KW; }
  {PP} ("tryundefine"|"tryundef"|"undefine"|"undef")             { pushState(PP_LINE); return PP_UNDEF_KW; }
  {PP} "ifdef"                                                   { pushState(PP_LINE); return PP_IFDEF_KW; }
  {PP} "ifndef"                                                  { pushState(PP_LINE); return PP_IFNDEF_KW; }
  {PP} "ifexp"                                                   { pushState(PP_RAW);  return PP_IFEXP_KW; }
  {PP} "else"                                                    { pushState(PP_RAW);  return PP_ELSE_KW; }
  {PP} "endif"                                                   { pushState(PP_RAW);  return PP_ENDIF_KW; }
  {PP} ("substdefs"|"substdef"|"subst")                          { pushState(PP_RAW);  return PP_SUBST_KW; }
  {PP} ("defexps"|"defexp"|"defenvs"|"defenv"|"trydefenvs"|"trydefenv") { pushState(PP_RAW); return PP_DEFENV_KW; }
  {PP} [a-zA-Z_][a-zA-Z0-9_]*                                    { pushState(PP_RAW);  return PP_UNKNOWN_KW; }
}

// ---------- Whitespace ----------
<YYINITIAL> {
  ({WS} | {CRLF})+          { return WHITE_SPACE; }
}

// Line terminators end the directive by switching the state back; the parser enforces
// line boundaries via the <<sameLine>> predicate (KamailioParserUtil), so the newline itself
// stays ordinary whitespace and does not disturb the formatter.
<PP_LINE> {
  {WS}+                     { return WHITE_SPACE; }
  \\ {CRLF}                 { return WHITE_SPACE; }   // line continuation inside a directive
  {CRLF}                    { stateStack.clear(); yybegin(YYINITIAL); return WHITE_SPACE; }
}

<PP_RAW> {
  {WS}+                     { return WHITE_SPACE; }
  {CRLF}                    { stateStack.clear(); yybegin(YYINITIAL); return WHITE_SPACE; }
  [^ \t\r\n] [^\r\n]*       { return PP_TEXT; }
}

// ---------- Comments ----------
<YYINITIAL, PP_LINE> {
  "//" [^\r\n]*             { return LINE_COMMENT; }
  "#"                       { return LINE_COMMENT; }
  "#" [^!\r\n] [^\r\n]*     { return LINE_COMMENT; }
  {BLOCK_COMMENT_FULL}      { return BLOCK_COMMENT; }
  {BLOCK_COMMENT_OPEN}      { return BLOCK_COMMENT; }   // unterminated at EOF
}

// ---------- event_route[name:with-colons] ----------
<YYINITIAL> {
  "event_route"             { pushState(EVRT); return EVENT_ROUTE_KW; }
}

<EVRT> {
  ({WS} | {CRLF})+          { return WHITE_SPACE; }
  "["                       { return LBRACK; }
  {EVENT_NAME_RE}           { return EVENT_NAME; }
  "]"                       { popState(); return RBRACK; }
  [^]                       { yypushback(1); popState(); }
}

// ---------- Keywords ----------
<YYINITIAL, PP_LINE> {
  "request_route"           { return REQUEST_ROUTE_KW; }
  "reply_route"             { return REPLY_ROUTE_KW; }
  "onreply_route"           { return ONREPLY_ROUTE_KW; }
  "failure_route"           { return FAILURE_ROUTE_KW; }
  "branch_route"            { return BRANCH_ROUTE_KW; }
  "onsend_route"            { return ONSEND_ROUTE_KW; }
  "route"                   { return ROUTE_KW; }
  "if"                      { return IF_KW; }
  "else"                    { return ELSE_KW; }
  "switch"                  { return SWITCH_KW; }
  "case"                    { return CASE_KW; }
  "default"                 { return DEFAULT_KW; }
  "while"                   { return WHILE_KW; }
  "break"                   { return BREAK_KW; }
  "return"                  { return RETURN_KW; }
  "exit"                    { return EXIT_KW; }
  "drop"                    { return DROP_KW; }
  "loadmodule"              { return LOADMODULE_KW; }
  "loadpath" | "mpath"      { return LOADPATH_KW; }
  "modparam" | "modparamx"  { return MODPARAM_KW; }
  "include_file"            { return INCLUDE_FILE_KW; }
  "import_file"             { return IMPORT_FILE_KW; }
  "desc"                    { return DESC_KW; }
}

// ---------- Literals ----------
<YYINITIAL, PP_LINE> {
  {IP4}                     { return IP_LITERAL; }
  {NUMBER_RE}               { return NUMBER; }
  {IDENT}                   { return IDENT; }
  \"                        { pushState(IN_DQ); return DQUOTE; }
  {SQ_STRING_FULL}          { return SQ_STRING; }
  {SQ_STRING_OPEN}          { return SQ_STRING; }   // unterminated at EOL/EOF
}

<IN_DQ> {
  \"                        { popState(); return DQUOTE; }
  \$\$                      { return STRING_TEXT; }
  {STR_PV}                  { return STRING_INTERP; }
  ( [^\"\\$\r\n] | \\[^\r\n] )+  { return STRING_TEXT; }
  \$                        { return STRING_TEXT; }
  \\                        { return STRING_TEXT; }
  {CRLF}                    { yypushback(yylength()); popState(); }   // unterminated string
}

// ---------- Operators & punctuation ----------
<YYINITIAL, PP_LINE> {
  "=="                      { return EQEQ; }
  "!="                      { return NEQ; }
  "=~"                      { return MATCH_OP; }
  "!~"                      { return NMATCH_OP; }
  "<="                      { return LE; }
  ">="                      { return GE; }
  "&&"                      { return ANDAND; }
  "||"                      { return OROR; }
  "=>"                      { return FATARROW; }
  "<"                       { return LT; }
  ">"                       { return GT; }
  "="                       { return EQ; }
  "!"                       { return NOT; }
  "+"                       { return PLUS; }
  "-"                       { return MINUS; }
  "*"                       { return STAR; }
  "/"                       { return SLASH; }
  "&"                       { return AMP; }
  "|"                       { return PIPE; }
  "$"                       { return DOLLAR; }
  "("                       { return LPAREN; }
  ")"                       { return RPAREN; }
  "{"                       { return LBRACE; }
  "}"                       { return RBRACE; }
  "["                       { return LBRACK; }
  "]"                       { return RBRACK; }
  ";"                       { return SEMICOLON; }
  ","                       { return COMMA; }
  ":"                       { return COLON; }
  "."                       { return DOT; }
}

<YYINITIAL, PP_LINE> {
  [^]                       { return BAD_CHARACTER; }
}
