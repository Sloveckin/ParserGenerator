package generator.lexer;

import java.util.List;

public record RuleCode(List<TerminalArg> rules, String code) { }