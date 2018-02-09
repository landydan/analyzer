import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tokenizer {

    private File file;
    private FileInputStream inputStream;
    private String currentToken;
    private List<String> words;
    private int wordIndex = 0;
    private String remainder = null;

    final static String START_COMMENT = "/*";
    final static String END_COMMENT = "*/";
    final static String ONE_COMMENT = "//";
    final static String QUOTE = "\"";

    public Tokenizer(File file) {
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


    public boolean hasMoreTokens() {
        return wordIndex < words.size() || remainder != null;
    }

    public void advance() {
        if (remainder == null) {
            currentToken = getNextToken(words.get(wordIndex));
        } else {
            currentToken = getNextToken(remainder);
        }
        ++wordIndex;
    }

    private String getNextToken(String word) {
        // temporary
        return word;
    }



    public TokenType tokenType() {
        return TokenType.KEYWORD;
    }


    public String getKeyword() {
        return currentToken;
    }

    public String getIdentifier() {
        return "keyword";
    }

    public String getString() {
        return "keyword";
    }


    public int getInteger() {
        return 0;
    }

    public char getSymbol() {
        return 'S';
    }


}
