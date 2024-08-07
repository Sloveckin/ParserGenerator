package generator;

public class ExceptionGenerator {
    final static String SOURCE =
            """
                    package %s;
                    public class ParserException extends Exception {
                                       public ParserException(final String message) {
                                           super(message);
                                       }
                                   }
                           """;


    public static String generate(final String path) {
       return String.format(SOURCE, path);
    }

}
