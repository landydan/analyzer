import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import sun.swing.StringUIClientPropertyKey;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class Tokenizer {

    private File file;
    private FileInputStream inputStream;
    private String currentToken;
    private TokenType currentTokenType;
    private List<String> words;
    private int wordIndex = -1;
    private String remainder = null;

    final static String START_COMMENT = "/*";
    final static String END_COMMENT = "*/";
    final static String ONE_COMMENT = "//";
    final static String QUOTE = "\"";

    private final static String symbolString = "(){}[].,;+-*/&|<>=~";

    private Map<String, String> translatorMap = new HashMap<String,String>();

    private final static Set<String> keywordSet = new HashSet<String>(Arrays.asList(
            "class", "constructor", "function", "method", "field", "static", "var", "int", "char",
            "boolean","void","true","false","null","this","let","do","if","else","while","return"));

    public Tokenizer(File file) {
        buildTranslatorMap();
        boolean insideMultilineComment = false;
        words = new ArrayList<String>();
        try {
            this.file = file;
            List<String> lines = FileUtils.readLines(file, "UTF-8");
            for (String l:lines) {
                String line = StringUtils.trimToEmpty(l);
                if (line.startsWith(START_COMMENT)) {
                    insideMultilineComment = true;
                }
                if (insideMultilineComment) {
                    if (line.endsWith(END_COMMENT)) {
                        insideMultilineComment = false;
                    }
                    continue;
                }
                line = StringUtils.substringBefore(line, ONE_COMMENT);
                if (!line.contains(QUOTE)) {
                    words.addAll(Arrays.asList(StringUtils.split(line)));
                } else {
                    words.addAll(getWordsInQuotedLine(line));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("could not open file");
        }
    }

    public List<String> getWordsInQuotedLine(String line) {
        List<String> words = new ArrayList<String>();
        boolean inside = false;
        String current = line;
        while (current.length() > 0) {
            String next = StringUtils.substringBefore(current, QUOTE);
            if (inside) {
                next = QUOTE + next + QUOTE;
            } else {
                next = StringUtils.trimToNull(next);
            }
            if (next != null) {
                words.add(next);
            }
            current = StringUtils.substringAfter(current, QUOTE);
            inside = !inside;
        }
        return words;
    }


    private void buildTranslatorMap() {
        translatorMap.put("&", "&amp;");
        translatorMap.put("<", "&lt;");
        translatorMap.put(">", "&gt;");
    }

    public boolean hasMoreTokens() {
        return wordIndex + 1 < words.size() || remainder != null;
    }

    public void advance() {
        if (remainder == null) {
            ++wordIndex;
            currentToken = getNextToken(words.get(wordIndex));
        } else {
            currentToken = getNextToken(remainder);
        }
    }

    private String getNextToken(String string) {
        if (string.startsWith(QUOTE)) {
            currentTokenType = TokenType.STRING_CONSTANT;
            return StringUtils.substringBetween(string, QUOTE, QUOTE);
        }
        String token = null;
        String firstChar = StringUtils.left(string, 1);
        if (StringUtils.isNumeric(firstChar)) {
            currentTokenType = TokenType.INTEGER_CONSTANT;
            token = getNumber(string);
        } else if (isSymbol(firstChar)) {
            currentTokenType = TokenType.SYMBOL;
            token = firstChar;
        } else {
           // it's either a keyword or an identifier
            int index = 0;
            String soFar = "";
            String nextChar = firstChar;
            while (index < string.length() && !StringUtils.isNumeric(nextChar) && !isSymbol(nextChar)) {
                soFar += nextChar;
                if (keywordSet.contains(soFar)) {
                    currentTokenType = TokenType.KEYWORD;
                    token = soFar;
                    break;
                }
                ++index;
                nextChar = StringUtils.substring(string, index, index + 1);
            }
            // if we made it to here we have an indentifier
            if (token == null) {
                currentTokenType = TokenType.IDENTIFIER;
                token = soFar;
            }
        }
        remainder = StringUtils.trimToNull(StringUtils.substringAfter(string, token));
        return translate(token);
    }

    private String translate(String token) {
        if (currentTokenType == TokenType.SYMBOL && translatorMap.keySet().contains(token)) {
            return translatorMap.get(token);
        }
        return token;
    }

    private boolean isSymbol(String string) {
        return symbolString.contains(string);
    }

    private String getNumber(String string) {
        int length = 0;
        while (length < string.length() && StringUtils.isNumeric(StringUtils.left(string, length+1))) {
            ++length;
        }
        return StringUtils.left(string, length);
    }


    public String getCurrentToken() {
        return currentToken;
    }

    public TokenType tokenType() {
        return currentTokenType;
    }

    String checkAndReturn(TokenType type) {
        if (currentTokenType != type) {
            throw new RuntimeException("unexpected type: " + type);
        }
        return currentToken;
    }

    public String getKeyword() {
        return checkAndReturn(TokenType.KEYWORD);
    }

    public String getIdentifier() {
        return checkAndReturn(TokenType.IDENTIFIER);
    }

    public String getString() {
        return checkAndReturn(TokenType.STRING_CONSTANT);
    }


    public String getInteger() {
        return checkAndReturn(TokenType.INTEGER_CONSTANT);
    }

    public String getSymbol() {
        return checkAndReturn(TokenType.SYMBOL);
    }


}
