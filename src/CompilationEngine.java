import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.*;

public class CompilationEngine {

    private InputStream input;
    private OutputStream output;

    private Tokenizer tokenizer;

    private final static Set<String> typeKeywords = new HashSet<String>(Arrays.asList("int", "char", "boolean"));

    public CompilationEngine(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    public CompilationEngine(Tokenizer tokenizer, OutputStream output) {
        this.output = output;
        this.tokenizer = tokenizer;
    }

    private void checkAndWriteExpected(TokenType type, String expected) {
        tokenizer.advance();
        if (tokenizer.tokenType() != type) {
            throw new IllegalArgumentException("expected token type: " + type);
        }
        String value = tokenizer.getCurrentToken();
        if (expected != null && !expected.equals(value)) {
            throw new IllegalArgumentException("expected value: " + expected);
        }
        writeLine(type.doTag(value));
    }

    private void beginTag(String tag) {
        writeLine(getBegin(tag));
    }

    private void endTag(String tag) {
        writeLine(getEnd(tag));
    }

    private String getBegin(String tag) {
       return "<" + tag + ">";
    }

    private String getEnd(String tag) {
        return "</" + tag + ">";
    }


    private void writeLine(String line) {
        Collection<String> lines = Collections.singletonList(line);
        try {
            IOUtils.writeLines(lines, JackAnalyzer.LINE_END, output, JackAnalyzer.UTF8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void compileClass() {
        beginTag("class");
        checkAndWriteExpected(TokenType.KEYWORD, "class");
        checkAndWriteExpected(TokenType.IDENTIFIER,  null);
        checkAndWriteExpected(TokenType.SYMBOL,  "{");

        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        while (isClassVar(token)) {
            compileClassVariableDeclaration(token);
            tokenizer.advance();
            token = tokenizer.getCurrentToken();
        }

        while (isSubroutine(token)) {
            compileSubroutineDeclaration(token);
            tokenizer.advance();
            token = tokenizer.getCurrentToken();
        }

        checkAndWriteExpected(TokenType.SYMBOL,  "}");
        endTag("class");
    }

    private boolean isClassVar(String token) {
        return (token.equals("static") || token.equals("field"));
    }

    private boolean isSubroutine(String token) {
        return (token.equals("constructor") || token.equals("function") || token.equals("method"));
    }

    public void compileClassVariableDeclaration(String keyword) {
        beginTag("classVarDec");
        writeLine(TokenType.KEYWORD.doTag(keyword));

        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        // type is int, char, boolean or className (identifier)
        if (typeKeywords.contains(token)) {
            writeLine(TokenType.KEYWORD.doTag(token));
        } else {
            writeLine(TokenType.IDENTIFIER.doTag(token));
        }

        while (!(token.equals(";"))) {
            tokenizer.advance();
            token = tokenizer.getCurrentToken();
            if (token.equals(",") || token.equals(";")) {
                writeLine(TokenType.SYMBOL.doTag(token));
            } else {
                writeLine(TokenType.IDENTIFIER.doTag(token));
            }
        }
        endTag("classVarDec");
    }

    public void compileSubroutineDeclaration(String keyword) {
        beginTag("subroutineDec");
        writeLine(TokenType.KEYWORD.doTag(keyword));

        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        if (token.equals("void")) {
            writeLine(TokenType.KEYWORD.doTag(token));
        } else if(typeKeywords.contains(token)) {
            writeLine(TokenType.KEYWORD.doTag(token));
        } else {
            writeLine(TokenType.IDENTIFIER.doTag(token));
        }
        checkAndWriteExpected(TokenType.IDENTIFIER, null);
        checkAndWriteExpected(TokenType.SYMBOL, "(");
        while (!(token.equals(")"))) {
            tokenizer.advance();
            token = tokenizer.getCurrentToken();
            if (token.equals(",") || token.equals(")")) {
                writeLine(TokenType.SYMBOL.doTag(token));
            } else if(typeKeywords.contains(token)) {
                writeLine(TokenType.KEYWORD.doTag(token));
            } else {
                writeLine(TokenType.IDENTIFIER.doTag(token));
            }
        }
        endTag("subroutineDec");
    }

    public void compileParameterList() {

    }

    public void compileSubroutineBody() {

    }

    // etc.


}
