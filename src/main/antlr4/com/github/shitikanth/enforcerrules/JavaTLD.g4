grammar JavaTLD;

compilationUnit : (typeDeclaration | .)* ;

typeDeclaration : ('class'|'interface'|'enum'|'@interface'|'record') ID .*? block ;

block :  '{' (block | .)*? '}' ;

WS : [ \r\t\n]+ -> skip ;

COMMENT      : '/*' .*? '*/'    -> skip;

LINE_COMMENT : '//' ~[\r\n]*    -> skip;

ID : [a-zA-Z$_] [a-zA-Z$_0-9]* ;

STRING_LITERAL: '"' (~["\\\r\n] | EscapeSequence)* '"';

TEXT_BLOCK: '"""' [ \t]* [\r\n] (. | EscapeSequence)*? '"""';

fragment EscapeSequence:
    '\\' 'u005c'? [btnfr"'\\]
    | '\\' 'u005c'? ([0-3]? [0-7])? [0-7]
    | '\\' 'u'+ HexDigit HexDigit HexDigit HexDigit
;

fragment HexDigit: [0-9a-fA-F];

ANY : . ;
