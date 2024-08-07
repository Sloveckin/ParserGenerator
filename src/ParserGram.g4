grammar ParserGram;

main: start all*;

all: makeTerminal | makeRule;

makeRule: QuestMark Var arguments? return? DoubleComma apply+ Code? End;

makeTerminal: Var DoubleComma (String | Reg) End;

apply: applyNotTerminal | applyTerminal;

applyNotTerminal: Excl Var Code1?;
applyTerminal: Var;

arguments: '[' arg otherArgs* ']';

start: 'START' Var '.';

arg: Var NAME;
otherArgs: Comma arg;


return: 'return' '[' arg ']';

Comma: ',';
Var: ([a-zA-Z'])+;
NAME: [a-zA-Z][a-zA-Z0-9]* ;
Code: '{' .*? '}';

Code1: '<' .*? '>';

//String: '/s/' .*? '/';
String: '"' .*? '"';
//Reg: '/r/' .*? '/';
Reg: '%' .*? '%';

WS: [ \n\r\t]+ -> skip;
DoubleComma: ':';
QuestMark: '?';
Excl: '!';
End: ';';