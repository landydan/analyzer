import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JackAnalyzer {

    private final static String SUFFIX = ".xml";
    private final static String UTF8 = "UTF-8";
    private final static String LINE_END = "\n";
    public final static String COMMENT = "//";

    private final static int INITIAL_STACK_POINTER = 256;

    public static void main(String[] args) {
        JackAnalyzer main = new JackAnalyzer();
        main.translate(args[0]);
    }

    private void translate(String inName) {
        String baseName = FilenameUtils.getBaseName(inName);
        String inDir = null;
        System.out.println("BASE:" + baseName);

        File arg = new File(inName);
        boolean isDirectory = arg.isDirectory();

        String outDir = "/Users/dan/temp/output";

        Collection<File> allFiles;
        if (isDirectory) {
            inDir = inName;
            allFiles = FileUtils.listFiles(arg, new String[]{"jack"}, false);
        } else {
            inDir = FilenameUtils.getFullPath(inName);
            allFiles = Collections.singletonList(arg);
        }
        FileOutputStream outputStream = null;
        try {
            for (File file:allFiles) {
                String outFullPath = FilenameUtils.concat(outDir, FilenameUtils.getBaseName(file.getName()) + SUFFIX);
                doOneFile(file, outFullPath);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("oops");
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

    }

    private void doOneFile(File file, String outPath) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(outPath);
            writeLine(out,  "<tokens>");
            Tokenizer tokenizer = new Tokenizer(file);

            /*
            String line = "    no quotes at all except \"this one at the end\"    ";
            for (String word:tokenizer.getWordsInQuotedLine(line)) {
                System.out.println(word);
            }
            */

            while (tokenizer.hasMoreTokens()) {
                String value = null;
                tokenizer.advance();
                TokenType type = tokenizer.tokenType();
                value = tokenizer.getCurrentToken();
                writeLine(out, type.doTag(value));
            }
            writeLine(out,  "</tokens>");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("oops");
        }
    }

    private void write(OutputStream outputStream, String value) throws Exception {
        IOUtils.write(value, outputStream, Charset.defaultCharset());
    }

    private void writeLine(OutputStream outputStream, String value) throws Exception {
        IOUtils.write(value + LINE_END, outputStream, Charset.defaultCharset());
    }



    private boolean skippable(String line) {
        return StringUtils.isBlank(line) || StringUtils.trimToEmpty(line).startsWith(COMMENT);
    }


}
