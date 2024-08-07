import exception.GeneratorException;
import generator.ExceptionGenerator;
import generator.lexer.*;
import generator.parser.NodeGenerator;
import generator.parser.ParserGenerator;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Generator extends ParserGramBaseVisitor<String> {
    private static final String DOLLAR = "$";
    private final Set<String> terminals = new HashSet<>();
    private final Set<String> notTerminals = new HashSet<>();
    private final Map<String, String> argFrom = new HashMap<>();
    private final Map<String, List<RuleCode>> table = new LinkedHashMap<>();
    private final Map<String, MyPair> mapping = new HashMap<>();
    private final Map<String, Set<String>> first = new HashMap<>();
    private final Map<String, Set<String>> follow = new HashMap<>();
    private String start;

    public void run(final String pack, final Path path, final String src) throws GeneratorException {
        init(src);
        constructFirstAndFollow();
        checkLL1();
        create(pack, path);
    }

    private void create(final String pack, final Path path) {
        final String stringLexer = LexerGenerator.generate(pack, mapping);
        final String stringToken = EnumGenerator.generate(pack, mapping);
        final String stringNode = NodeGenerator.generate(pack);
        final String stringParser = new ParserGenerator(table, notTerminals, first, argFrom, pack, start).generate();
        final String stringException = ExceptionGenerator.generate(pack);
        createTokens(path, stringToken);
        createLexer(path, stringLexer);
        createNode(path, stringNode);
        createParser(path, stringParser);
        createExceptionFile(path, stringException);
    }


    private void createNode(final Path path, final String source) {
        createFile(path, source, "Node.java");
    }

    private void createParser(final Path path, final String source) {
        createFile(path, source, "Parser.java");
    }

    private void createLexer(final Path path, final String source) {
        createFile(path, source, "Lexer.java");
    }

    private void createTokens(final Path path, final String source) {
        createFile(path, source, "Token.java");
    }

    private void createExceptionFile(final Path path, final String source) {
        createFile(path, source, "ParserException.java");
    }

    private void createFile(final Path path, final String source, final String file) {
        try {
            Files.writeString(path.resolve(file), source);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void init(final String src) {
        ParserGramLexer lexer = new ParserGramLexer(CharStreams.fromString(src));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ParserGramParser parser = new ParserGramParser(tokens);

        ParseTree tree = parser.main();

        visit(tree);
    }

    private void constructFirstAndFollow() {
        constructFIRST();
        constructFOLLOW();
    }

    @Override
    public String visitReturn(ParserGramParser.ReturnContext ctx) {
        return visit(ctx.arg());
    }

    private void constructFIRST() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (final var entry : table.entrySet()) {

                if (!first.containsKey(entry.getKey())) {
                    first.put(entry.getKey(), new HashSet<>());
                }

                for (var subList : entry.getValue()) {
                    final String word = subList.rules().getFirst().Terminal();
                    if (first.get(entry.getKey()).contains(word)) continue;

                    if (terminals.contains(word)) {
                        first.get(entry.getKey()).add(word);
                        changed = true;
                    } else {
                        final Set<String> hlp = first.get(word);
                        if (hlp == null) continue;
                        changed ^= first.get(entry.getKey()).addAll(hlp);
                    }
                }
            }
        }

    }

    private void constructFOLLOW() {
        table.keySet().forEach(el -> follow.put(el, new HashSet<>()));
        follow.get(start).add(DOLLAR);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (final var entry : table.entrySet()) {
                var key = entry.getKey();

                var list = entry.getValue();
                for (var subList : list) {
                    for (int i = 0; i < subList.rules().size(); i++) {
                        var size = subList.rules().size();
                        final String curEl = subList.rules().get(i).Terminal();
                        if (terminals.contains(curEl)) {
                            continue;
                        }

                        if (i == size - 1) {
                            changed = follow.get(curEl).addAll(follow.get(key));
                            continue;
                        }

                        var nextEl = subList.rules().get(i + 1).Terminal();
                        if (terminals.contains(nextEl)) {
                            follow.get(curEl).add(nextEl);
                            continue;
                        }
                        var f = new HashSet<>(first.get(nextEl));
                        var hasEps = f.remove("Eps");
                        var ch1 = follow.get(curEl).addAll(f);
                        boolean ch2 = false;
                        if (hasEps) {
                            ch2 = follow.get(curEl).addAll(follow.get(key));
                        }
                        changed = ch1 || ch2;
                    }
                }

            }
        }
    }


    @Override
    public String visitStart(ParserGramParser.StartContext ctx) {
        start = ctx.Var().toString();
        return null;
    }

    @Override
    public String visitMain(ParserGramParser.MainContext ctx) {
        visit(ctx.start());
        ctx.all().forEach(this::visit);
        return null;
    }

    @Override
    public String visitAll(ParserGramParser.AllContext ctx) {
        if (ctx.makeTerminal() != null) {
            visit(ctx.makeTerminal());
        } else {
            visit(ctx.makeRule());
        }
        return null;
    }


    @Override
    public String visitMakeRule(ParserGramParser.MakeRuleContext ctx) {
        final String key = ctx.Var().toString();
        notTerminals.add(key);

        if (ctx.arguments() != null) {
            argFrom.put(key, visit(ctx.arguments()));
        } else {
            argFrom.put(key, "");
        }


        String code = "";
        if (ctx.Code() != null) {
            code = ctx.Code().toString();
            code = code.substring(1, code.length() - 1);
        }

        if (!table.containsKey(key)) {
            table.put(key, new ArrayList<>());
        }

        final List<TerminalArg> list = new ArrayList<>();
        ctx.apply().forEach(el -> {
            var arr = visit(el).split(":");
            if (arr.length == 1) {
                list.add(new TerminalArg(arr[0], ""));
            } else {
                final String s = arr[1];
                list.add(new TerminalArg(arr[0], s.substring(1, s.length() - 1)));
            }
        });
        table.get(key).add(new RuleCode(list, code));
        return null;
    }


    @Override
    public String visitArguments(ParserGramParser.ArgumentsContext ctx) {
        final List<String> list = new ArrayList<>();
        list.add(visit(ctx.arg()));
        ctx.otherArgs().forEach(el -> list.add(visit(el)));
        return String.join(", ", list);
    }

    @Override
    public String visitArg(ParserGramParser.ArgContext ctx) {
        return ctx.Var() + " " + ctx.NAME().toString();
    }

    @Override
    public String visitOtherArgs(ParserGramParser.OtherArgsContext ctx) {
        return super.visitOtherArgs(ctx);
    }

    @Override
    public String visitMakeTerminal(ParserGramParser.MakeTerminalContext ctx) {
        final String k = ctx.Var().toString();
        terminals.add(k);
        if (ctx.Reg() != null) {
            String reg = ctx.Reg().getText();
            reg = reg.substring(1, reg.length() - 1);
            mapping.put(k, new MyPair(true, reg));
        } else {
            String notReg = ctx.String().getText();
            notReg = notReg.substring(1, notReg.length() - 1);
            mapping.put(k, new MyPair(false, notReg));
        }

        return null;
    }

    @Override
    public String visitApply(ParserGramParser.ApplyContext ctx) {
        if (ctx.applyTerminal() != null) {
            return visit(ctx.applyTerminal());
        }
        return visit(ctx.applyNotTerminal());
    }

    @Override
    public String visitApplyNotTerminal(ParserGramParser.ApplyNotTerminalContext ctx) {
        final StringBuilder sb = new StringBuilder();
        sb.append(ctx.Var().toString());
        if (ctx.Code1() != null) {
            sb.append(":").append(ctx.Code1().toString());
        }
        return sb.toString();
    }

    @Override
    public String visitApplyTerminal(ParserGramParser.ApplyTerminalContext ctx) {
        return ctx.Var().toString();
    }


    private void checkLL1() throws GeneratorException {
        /// 1
        for (var entry : table.entrySet()) {
            final Set<String> hlp = new HashSet<>();
            for (final RuleCode rule : entry.getValue()) {
                final Set<String> hhh = getSafeFirst(rule.rules().getFirst().Terminal());
                for (final String el : hhh) {
                    if (hlp.contains(el)) {
                        //throw new GeneratorException("Not LL(1) grammar");
                        throw new GeneratorException("hehe");
                    }
                    hlp.add(el);
                }
            }
        }

        /// 2
        final Set<String> needCheck = new HashSet<>();
        for (var entry : table.entrySet()) {
            final String k = entry.getKey();
            for (final RuleCode rule : entry.getValue()) {
                final String eps = rule.rules().getFirst().Terminal();
                if (eps.equals("Eps")) {
                    needCheck.add(k);
                }
            }
        }

        for (final String el : needCheck) {
            final Set<String> hlp1 = new HashSet<>();
            var t = table.get(el);
            for (var tt : t) {
                var s = tt.rules().getFirst().Terminal();
                hlp1.add(s);
            }

            for (final String a : hlp1) {
                for (final String b : hlp1) {
                    if (a.equals(b)) continue;

                    final Set<String> followA = getSafeFollow(a);
                    final Set<String> firstB = getSafeFirst(b);

                    final Set<String> hlp = new HashSet<>(followA);
                    hlp.retainAll(firstB);
                    if (!hlp.isEmpty()) {
                        throw new GeneratorException("Not LL(1) grammar");
                    }
                }
            }
        }
    }

    public Set<String> getSafeFollow(final String src) {
       if (terminals.contains(src)) {
           return new HashSet<>();
       }
       return follow.get(src);
    }

    public Set<String> getSafeFirst(final String src) {
       if (terminals.contains(src)) {
           final Set<String> r = new HashSet<>();
           r.add(src);
           return r;
       }
       return first.get(src);
    }


    /// Add checking exception
    public static void main(String[] args) throws IOException {
        final String src = Files.readString(Paths.get(args[0]));
        final Path path = Paths.get("./generated", args[1]);
        final String pack = args[1];
        try {
            new Generator().run(pack, path, src);
        } catch (final GeneratorException e) {
            System.out.println("Error while generating. Cause: " + e.getMessage());
        }
    }

}


