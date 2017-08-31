package eu.metatools.dbs;

import com.google.common.base.Charsets;
import com.google.common.io.CharSink;
import com.google.common.io.Files;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

public class Processor {
    public static void main(String[] args) throws IOException {
        transform("english_words_all");
        transform("german_words_all");
    }

    private static void transform(String file) throws IOException {
        int length = 3;
        String prefix = null;

        Writer writer = null;

        new File(file).mkdir();

        List<String> shortWords = new ArrayList<String>();
        for (String s : Files.asCharSource(new File(file + ".json"), Charsets.UTF_8).readLines()) {
            int start = s.indexOf('"');
            if (start >= 0) {
                int end = s.indexOf('"', start + 1);
                if (end >= 0) {
                    String word = s.substring(start + 1, end);
                    if (word.length() < length)
                        shortWords.add(word);
                    else {
                        if (prefix == null || !word.startsWith(prefix)) {
                            prefix = word.substring(0, Math.min(length, word.length()));
                            if (writer != null)
                                writer.close();

                            writer = Files.newWriter(new File(new File(file), prefix), Charsets.UTF_8);
                        }

                        writer.append(word);
                        writer.append('\r');
                        writer.append('\n');
                    }
                }
            }
        }

        Files.asCharSink(new File(new File(file), ".shortnames"), Charsets.UTF_8).writeLines(shortWords);
    }

}
