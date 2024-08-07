package generator.parser;

import generator.lexer.RuleCode;
import generator.lexer.TerminalArg;

import java.util.*;

public class ParserGenerator {

    private static final String SOURCE =
          """ 
                  package %s;
                                       
                  import java.io.InputStream;
                  import java.util.ArrayList;
                  import java.util.List;
                             
                  public class Parser {
                       private final Lexer lexer;
                       
                       public Parser(final InputStream stream) throws ParserException {
                           this.lexer = new Lexer(stream);
                           this.lexer.nextToken();
                       }
                       
                       public Node parse() throws ParserException {
                            return %s();
                       }
                       
                              
                       private Node makeNode(final String name, final Node... list) {
                            return new Node(name, list);
                       }
                       
                       
                       %s
                  }""";

    private final Set<String> notTerminal;
    private final Map<String, Set<String>> first;
    private final Map<String, List<RuleCode>> table;


    private final Map<String, String> args;
    private final String pack;

    private final String start;

    public ParserGenerator(final Map<String, List<RuleCode>> table,
                           final Set<String> notTerminal,
                           final Map<String, Set<String>> first,
                           final Map<String, String> args,
                           final String pack,
                           final String start) {
        this.notTerminal = notTerminal;
        this.first = first;
        this.pack = pack;
        this.table = table;
        this.args = args;
        this.start = start;
    }

    public String generate() {
        final List<String> methods = notTerminal.stream().map(this::generateMethod).toList();
        return String.format(SOURCE, pack, start, String.join("\n", methods));
    }

    private static final String METHOD_SOURCE =
            """
            private Node %s(%s) throws ParserException {
                final Node res = new Node("%s");
                final Token token = lexer.getCurToken();
                %s
                %s;
            }
            """;
    private String generateMethod(final String notTerminal) {
        final String block = ifBlock(notTerminal);
        final String eps = epss(notTerminal);
        final String lArgs = args.get(notTerminal);
        return String.format(METHOD_SOURCE, notTerminal, lArgs, notTerminal,  block, eps);
    }


    private static final String CASE_BLOCK =
           """
           \t\tif(%s) {
            \t\t\t%s
            \t\t\t%s
            \t\t\t return res;
           \t\t}
           """;

    private String ifBlock(final String notTerminal) {
        final List<RuleCode> rulesWithCode = table.get(notTerminal);
        final Set<String> localFirst = first.get(notTerminal);

        final List<String> cases = new ArrayList<>();

        boolean flag = true;

        for (final RuleCode subList : rulesWithCode) {
            final String afterArrow = subList.rules().getFirst().Terminal();

            if (localFirst.contains(afterArrow)) {
                if (afterArrow.equals("Eps")) continue;

                final List<String> l = new ArrayList<>();
                for (int i = 0; i < subList.rules().size(); i++) {
                   l.add(addingInRes(subList.rules().get(i).Terminal(), i, subList.rules().get(i).args()));
                }

                final String h = String.join("\n", l);
                cases.add(String.format(CASE_BLOCK, "token == Token." + afterArrow, h, subList.code()));
                flag = false;
            }
        }
        if (flag) {
            final List<String> hlp = localFirst.stream().map(el -> "token == Token." + el).toList();
            final String args = String.join(" || ", hlp);

            final List<String> l = new ArrayList<>();
            final var ll = rulesWithCode.getFirst().rules();
            for (int i = 0; i < ll.size(); i++) {
                l.add(addingInRes(ll.get(i).Terminal(), i, ll.get(i).args()));
            }

            final String h = String.join("\n", l);

            return String.format(CASE_BLOCK, args, h, rulesWithCode.getFirst().code());
        }

        return String.join("\n", cases);
    }

    private String epss(final String notTerminal) {
        int index = -1;
        final List<List<TerminalArg>> ttt = table.get(notTerminal).stream().map(RuleCode::rules).toList();
        for (int i = 0; i < ttt.size(); i++) {
            for (int j = 0; j < ttt.get(i).size(); j++) {
               if (ttt.get(i).get(j).Terminal().contains("Eps")) {
                   index = i;
                   break;
               }
            }
        }

        if (index >= 0) {
            final String code = table.get(notTerminal).get(index).code();

            final String ss =
                    """
                    final Node epsNode = new Node("%s");
                    epsNode.getChildren().add(new Node("Eps"));
                    %s
                    return epsNode
                    """;

            return String.format(ss, notTerminal, code);
        }
        return String.format("throw new RuntimeException(\"Not expected token in %s. Token = \" + lexer.getCurText())",
                notTerminal);
    }


    private String addingInRes(final String src, int index, final String args) {
        final String variable = "var" + index;
        if (notTerminal.contains(src)) {
           final String f = String.format("final Node %s = %s(%s);", variable, src, args);
           final String s =  String.format("res.getChildren().add(var%s);",  index);
           return String.join("\n", f, s);
        }

       final String r =
                """
                final Node %s = new Node(lexer.getCurText());
                res.getChildren().add(%s);
                lexer.nextToken();
                """;
       return String.format(r, variable, variable);
    }
}
