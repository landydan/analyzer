import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CompilationEngine {

    private InputStream input;
    private OutputStream output;

    private Tokenizer tokenizer;

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

        compileSubroutineDeclaration();
        checkAndWriteExpected(TokenType.SYMBOL,  "}");
        endTag("class");
    }

    private boolean isClassVar(String token) {
        return (token.equals("static") || token.equals("field"));
    }

    public void compileClassVariableDeclaration(String keyword) {
        beginTag("classVarDec");
        writeLine(TokenType.KEYWORD.doTag(keyword));

        endTag("classVarDec");
    }

    public void compileSubroutineDeclaration() {
        writeLine("SUBROUTINE DEC HERE");
    }

    public void compileParameterList() {

    }

    public void compileSubroutineBody() {

    }

    // etc.


}
