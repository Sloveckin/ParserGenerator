package generator.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnumGenerator  {
    private static final String source = """
            
            package %s;
            
            public enum Token {
                %s;
                
                private final String text;
                
                private final boolean isRegex;
                
                private Token(final String text, final boolean isRegex) {
                   this.text = text;
                   this.isRegex = isRegex;
                }
                
                public String getText() {
                    return text;
                }
                
                public boolean getIsRegex() {
                    return isRegex;
                }
                
            }
            """;

    public static String generate(final String pack, final Map<String, MyPair> map) {

        final List<String> list = new ArrayList<>();

        map.forEach((k, v) -> {
            final String source = "%s(\"%s\", %s)";
            final String res = String.format(source, k, v.text(), v.isRegex());
            list.add(res);
        });

        list.add("END(\"$\", false)");

        final String r = String.join(", ", list);
        return String.format(source, pack, r);
    }

}
