package generator.lexer;

import java.util.List;
import java.util.Map;

public class LexerGenerator {

    private static final String BEFORE =
            """
            if (%s) {
                sb.append((char) curChar);
                break;
            }
            """;

    private static final String AFTER =
            """
             if (%s) {
                notSkip = true;
                break;
            }
            """;


    private static final String SOURCE = """
            package %s;
                        
            import java.io.IOException;
            import java.io.InputStream;
            import java.util.regex.Pattern;
            import java.util.regex.Matcher;
                        
                        
            public class Lexer {
                private final InputStream stream;
                private Token curToken;
                private int curChar;
                
                private String curText;
                
                
                public Lexer(final InputStream stream) {
                   this.stream = stream;
                   nextChar();
                }
                        
                public Token getCurToken() {
                    return curToken;
                }
                
                public String getCurText() {
                    return curText;
                }
                
                public int getCurChar() {
                    return curChar;
                }
                        
                private boolean isBlank(final int ch) {
                    return Character.isWhitespace(ch);
                }
                        
                private void nextChar() {
                    try {
                       curChar = stream.read();
                    } catch (final IOException e) {
                       throw new RuntimeException(e);
                    }
                }
                
                private boolean notSkip = false;
                
                public void nextToken() throws ParserException  {
                    if (!notSkip) {
                        while (isBlank(curChar)) {
                            nextChar();
                        }
                    }
                    
                    final StringBuilder sb = new StringBuilder();
                    while (!isBlank(curChar)) {
                        if (curChar == -1) {
                            curToken = Token.END;
                            curText = "$";
                            return;
                        }
                             
                        if (notSkip) {
                            notSkip = false;
                        }
                       
                        final String hlp = String.valueOf((char) curChar);
                        
                        %s
                        
                        sb.append((char) curChar);
                        nextChar();
                        
                        final String hlp1 = String.valueOf((char) curChar);
                        %s
                        
                    }
                    final String res = sb.toString();
                    
                    if (!notSkip) {
                        nextChar();
                    }
                    
                    %s
                    
                    throw new ParserException("Not expected " + res);
                }
            }
            """;

        public static String generate(final String pack, final Map<String, MyPair> map) {
            final String cases = new CaseGenerator().generate(map);
            final List<String> arr = map.values().stream().map(MyPair::text).toList();
            final String before = beforeOrAfter(arr, BEFORE, "hlp");
            final String after = beforeOrAfter(arr, AFTER, "hlp1");
            return String.format(SOURCE, pack, before, after , cases);
        }

        private static String beforeOrAfter(final List<String> arr, final String source, final String hlp) {
            final List<String> list = arr.stream().map((el) -> hlp + ".equals(\"" + el + "\")").toList();
            final String inIf = String.join(" || ", list);
            return String.format(source, inIf);
        }
}