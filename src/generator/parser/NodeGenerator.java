package generator.parser;

public class NodeGenerator {

    private static final String SOURCE =
            """
            package %s;
            import java.util.Arrays;
            import java.util.Collections;
            import java.util.List;
            import java.util.ArrayList;
                                        
            public class Node {
                private final String text;
                private final List<Node> children;
                
                public double val;
                                        
               public Node(final String text, final Node... children) {
                    this.text = text;
                    this.children = new ArrayList<>(Arrays.asList(children));
               }
                                       
               public Node(final String text) {
                    this.text = text;
                    this.children = new ArrayList<>(Collections.emptyList());
               }
                                        
               public List<Node> getChildren() {
                    return children;
               }
                                        
               public String getText() {
                    return text;
               }
            }
            """;



    public static String generate(final String pack) {
       return String.format(SOURCE, pack);
    }


}
