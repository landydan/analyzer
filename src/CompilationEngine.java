import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class CompilationEngine {

    private InputStream input;
    private OutputStream output;

    private Tokenizer tokenizer;

    private final static Set<String> typeKeywords = new HashSet<String>(Arrays.asList("int", "char", "boolean"));

    private final static Set<String> statementKeywords = new HashSet<String>(Arrays.asList("if", "let", "while", "do", "return"));

    private final static Set<String> operations = new HashSet<String>(Arrays.asList("+", "-", "*", "/", "&", "|", "<", ">", "=" ));

    private final static Set<String> unaryOps = new HashSet<String>(Arrays.asList("~", "-"));


    public CompilationEngine(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    public CompilationEngine(Tokenizer tokenizer, OutputStream output) {
        this.output = output;
        this.tokenizer = tokenizer;
    }

    private void advanceAndWriteExpected(TokenType type, String expected) {
        tokenizer.advance();
        writeExpected(type, expected);
    }

    private void writeExpected(TokenType type, String expected) {
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
        advanceAndWriteExpected(TokenType.KEYWORD, "class");
        advanceAndWriteExpected(TokenType.IDENTIFIER, null);
        advanceAndWriteExpected(TokenType.SYMBOL, "{");

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

        writeExpected(TokenType.SYMBOL, "}");
        endTag("class");
    }

    private boolean isClassVar(String token) {
        return (token.equals("static") || token.equals("field"));
    }

    private boolean isStatement(String token) {
        return statementKeywords.contains(token);
    }

    private boolean isSubroutine(String token) {
        return (token.equals("constructor") || token.equals("function") || token.equals("method"));
    }

    private boolean isOperation(String token) {
        return operations.contains(token);
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
        } else if (typeKeywords.contains(token)) {
            writeLine(TokenType.KEYWORD.doTag(token));
        } else {
            writeLine(TokenType.IDENTIFIER.doTag(token));
        }
        advanceAndWriteExpected(TokenType.IDENTIFIER, null);

        advanceAndWriteExpected(TokenType.SYMBOL, "(");
        compileParameterList();
        writeExpected(TokenType.SYMBOL, ")");
        compileSubroutineBody();

        endTag("subroutineDec");
    }

    public void compileParameterList() {
        beginTag("parameterList");
        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        while (!(token.equals(")"))) {
            if (token.equals(",")) {
                writeLine(TokenType.SYMBOL.doTag(token));
            } else if (typeKeywords.contains(token)) {
                writeLine(TokenType.KEYWORD.doTag(token));
            } else {
                writeLine(TokenType.IDENTIFIER.doTag(token));
            }
            tokenizer.advance();
            token = tokenizer.getCurrentToken();
        }
        endTag("parameterList");
    }

    public void compileSubroutineBody() {
        beginTag("subroutineBody");
        advanceAndWriteExpected(TokenType.SYMBOL, "{");

        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        while (token.equals("var")) {
            compileVarDec();
            tokenizer.advance();
            token = tokenizer.getCurrentToken();
        }
        compileStatements(token);
        writeExpected(TokenType.SYMBOL, "}");

        endTag("subroutineBody");
    }

    public void compileVarDec() {
        beginTag("varDec");

        writeLine(TokenType.KEYWORD.doTag("var"));
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

        endTag("varDec");
    }


    public void compileStatements(String token) {
        beginTag("statements");
        boolean needAdvance = true;
        while (isStatement(token)) {
            if (token.equals("if")) {
                compileIf();
                needAdvance = false;
            }
            if (token.equals("let")) {
                compileLet();
                needAdvance = true;
            }
            if (token.equals("do")) {
                compileDo();
                needAdvance = true;
            }
            if (token.equals("while")) {
                compileWhile();
                needAdvance = false;
            }
            if (token.equals("return")) {
                compileReturn();
                needAdvance = true;
            }
            if (needAdvance) {
                tokenizer.advance();
            }
            token = tokenizer.getCurrentToken();
        }


        endTag("statements");
    }


    public void compileIf() {
        beginTag("ifStatement");
        writeLine(TokenType.KEYWORD.doTag("if"));
        advanceAndWriteExpected(TokenType.SYMBOL, "(");
        tokenizer.advance();
        compileExpression();
        writeExpected(TokenType.SYMBOL, ")");
        advanceAndWriteExpected(TokenType.SYMBOL, "{");
        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        compileStatements(token);
        writeExpected(TokenType.SYMBOL, "}");
        tokenizer.advance();
        token = tokenizer.getCurrentToken();
        if (token.equals("else")) {
            writeLine(TokenType.KEYWORD.doTag("else"));
            advanceAndWriteExpected(TokenType.SYMBOL, "{");
            tokenizer.advance();
            token = tokenizer.getCurrentToken();
            compileStatements(token);
            writeExpected(TokenType.SYMBOL, "}");
            tokenizer.advance();
        }
        endTag("ifStatement");
        return;
    }

    public void compileLet() {
        beginTag("letStatement");
        writeLine(TokenType.KEYWORD.doTag("let"));
        advanceAndWriteExpected(TokenType.IDENTIFIER, null);
        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        if (token.equals("[")) {
            writeLine(TokenType.SYMBOL.doTag("["));
            tokenizer.advance();
            compileExpression();
            writeExpected(TokenType.SYMBOL, "]");
            tokenizer.advance();
        }
        writeExpected(TokenType.SYMBOL, "=");
        tokenizer.advance();
        compileExpression();
        writeExpected(TokenType.SYMBOL, ";");
        endTag("letStatement");
    }

    public void compileDo() {
        beginTag("doStatement");
        writeLine(TokenType.KEYWORD.doTag("do"));

        // subroutine name, or class or var name
        advanceAndWriteExpected(TokenType.IDENTIFIER, null);
        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        if (token.equals(".")) {
            writeLine(TokenType.SYMBOL.doTag("."));
            // subroutine name
            advanceAndWriteExpected(TokenType.IDENTIFIER, null);
            tokenizer.advance();
            token = tokenizer.getCurrentToken();
        }
        writeExpected(TokenType.SYMBOL, "(");
        compileExpressionList();
        writeExpected(TokenType.SYMBOL, ")");
        advanceAndWriteExpected(TokenType.SYMBOL, ";");
        endTag("doStatement");
    }

    public void compileWhile() {
        beginTag("whileStatement");
        writeLine(TokenType.KEYWORD.doTag("while"));
        advanceAndWriteExpected(TokenType.SYMBOL, "(");
        tokenizer.advance();
        compileExpression();
        writeExpected(TokenType.SYMBOL, ")");
        advanceAndWriteExpected(TokenType.SYMBOL, "{");
        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        compileStatements(token);
        writeExpected(TokenType.SYMBOL, "}");
        tokenizer.advance();
        token = tokenizer.getCurrentToken();
        endTag("whileStatement");
    }

    public void compileReturn() {
        beginTag("returnStatement");
        writeLine(TokenType.KEYWORD.doTag("return"));
        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        if (!token.equals(";")) {
            compileExpression();
        }
        writeExpected(TokenType.SYMBOL, ";");
        endTag("returnStatement");
    }

    // current token is the open paren
    // caller will write the closed paren
    public void compileExpressionList() {
        beginTag("expressionList");
        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        while (!token.equals(")")) {
            if (token.equals(",")) {
                writeExpected(TokenType.SYMBOL, ",");
                tokenizer.advance();
                token = tokenizer.getCurrentToken();
            }
            compileExpression();
            token = tokenizer.getCurrentToken();
        }
        endTag("expressionList");
    }

    // this assumes "advance" has already happened
    public void compileExpression() {
        beginTag("expression");
        compileTerm();
        tokenizer.advance();
        String token = tokenizer.getCurrentToken();
        if (isOperation(token)) {
            writeExpected(TokenType.SYMBOL, token);
            tokenizer.advance();
            compileTerm();
            tokenizer.advance();
        }
        endTag("expression");
    }

    // assume we've already advanced
    public void compileTerm() {
        beginTag("term");
        String token = tokenizer.getCurrentToken();
        TokenType type = tokenizer.tokenType();
        writeLine(type.doTag(token));
        endTag("term");
    }


}