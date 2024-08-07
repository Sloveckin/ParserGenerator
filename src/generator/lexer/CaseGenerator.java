package generator.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaseGenerator {

    private static final String NOT_REG = """
            \t\tif (res.equals("%s")) {
                \t\tcurToken = Token.%s;
                \t\tcurText = "%s";
                \t\treturn;
            \t\t}
            """;

    private static final String REG = """
           \t\tPattern p%s = Pattern.compile("%s");
           \t\tMatcher m%s = p%s.matcher(res);
           \t\tif (m%s.matches()) {
                \t\tcurToken = Token.%s;
                \t\tcurText = res;
                \t\treturn;
           \t\t}
            """;

    public String generate(final Map<String, MyPair> mp) {
        final List<String> list = new ArrayList<>();

        final Map<String, MyPair> notRegex = new HashMap<>();
        final Map<String, MyPair> regex = new HashMap<>();


        mp.forEach((k, v) -> {
            if (v.isRegex()) {
                regex.put(k, v);
            } else {
                notRegex.put(k, v);
            }
        });

        notRegex.forEach((k, v) -> {
            final String res =  String.format(NOT_REG, v.text(), k, v.text());
            list.add(res);
        });

        regex.forEach((k, v) -> {
            final String reg = v.text();
            final String res = String.format(REG, k, reg, k, k, k, k);
            list.add(res);
        });


        return String.join("\n", list);
    }
}
