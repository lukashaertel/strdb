package eu.metatools.dbs;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import java.io.*;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StrDb {
    /**
     * The ZIP file containing the prefixes.
     */
    private final File file;

    /**
     * The base path within the ZIP file.
     */
    private final String basePath;

    /**
     * The length of the prefix.
     */
    private final int prefixLength;

    /**
     * Constructs the string DB.
     *
     * @param file         The ZIP file containing the prefixes.
     * @param basePath     The base path within the ZIP file.
     * @param prefixLength The length of the prefix.
     */
    public StrDb(File file, String basePath, int prefixLength) {

        this.file = file;
        this.basePath = basePath;
        this.prefixLength = prefixLength;
    }

    /**
     * Provides all words to the consumer.
     *
     * @param consumer The consumer to feed with the words.
     * @throws IOException Thrown from the underlying implementations.
     */
    public void words(Consumer<String> consumer) throws IOException {
        try (ZipFile zipFile = new ZipFile(file, Charsets.UTF_8)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                new ByteSource() {
                    @Override
                    public InputStream openStream() throws IOException {
                        return zipFile.getInputStream(entry);
                    }
                }.asCharSource(Charsets.UTF_8).forEachLine(consumer);
            }
        }
    }

    /**
     * Processes all words, stops when the corresponding processLine method returns false.
     *
     * @param processor The processor to feed the lines into.
     * @param <T>       The type of the result.
     * @return Returns the result of the processor.
     * @throws IOException Thrown from the underlying implementations.
     */
    public <T> T words(LineProcessor<T> processor) throws IOException {
        try (ZipFile zipFile = new ZipFile(file, Charsets.UTF_8)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                try (BufferedReader reader = new ByteSource() {
                    @Override
                    public InputStream openStream() throws IOException {
                        return zipFile.getInputStream(entry);
                    }
                }.asCharSource(Charsets.UTF_8).openBufferedStream()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!processor.processLine(line))
                            return processor.getResult();
                    }
                }
            }
        }

        return processor.getResult();
    }

    /**
     * Counts all words in the database.
     *
     * @return Returns the count of words in the database.
     * @throws IOException Thrown from the underlying implementations.
     */
    public int getCount() throws IOException {
        return words(new LineProcessor<Integer>() {
            int count = 0;

            @Override
            public boolean processLine(String line) throws IOException {
                count++;
                return true;
            }

            @Override
            public Integer getResult() {
                return count;
            }
        });
    }


    /**
     * Returns a list of all words where the predicate is true.
     *
     * @param predicate The predicate to test for.
     * @return Returns an immutable list of words where the predicate was true.
     * @throws IOException Thrown from the underlying implementations.
     */
    public ImmutableList<String> where(final Predicate<String> predicate) throws IOException {
        return where(predicate, -1);
    }


    /**
     * Returns a list of all words where the predicate is true.
     *
     * @param predicate The predicate to test for.
     * @param limit     The limit of results or -1 if unlimited.
     * @return Returns an immutable list of words where the predicate was true.
     * @throws IOException Thrown from the underlying implementations.
     */
    public ImmutableList<String> where(final Predicate<String> predicate, final int limit) throws IOException {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();

        // Process all words, test predicate and add to builder if predicate was true.
        words(new LineProcessor<Void>() {
            private int found = 0;

            @Override
            public boolean processLine(String line) throws IOException {
                if (predicate.test(line)) {
                    builder.add(line);
                    found++;
                    if (limit != -1 && found >= limit)
                        return false;
                }

                return true;
            }

            @Override
            public Void getResult() {
                return null;
            }
        });

        return builder.build();
    }

    /**
     * Returns true if the word in any casing is contained in the database.
     *
     * @param word The word to look for.
     * @return Returns true if the word is contained.
     * @throws IOException Thrown from the underlying implementations.
     */
    public boolean contains(String word) throws IOException {
        return resolve(word, false) != null;
    }

    /**
     * Returns true if the word is contained in the database, respects casing if required.
     *
     * @param word      The word to look for.
     * @param matchCase True if casing is to be acknowledged.
     * @return Returns true if the word is contained.
     * @throws IOException Thrown from the underlying implementations.
     */
    public boolean contains(String word, boolean matchCase) throws IOException {
        return resolve(word, matchCase) != null;
    }

    /**
     * Gets the word in the database that matches the given word in any casing.
     *
     * @param word The word to look for.
     * @return Returns The word in the actual casing.
     * @throws IOException Thrown from the underlying implementations.
     */
    public String resolve(String word) throws IOException {
        return resolve(word, false);
    }

    /**
     * Gets the word in the database that matches the given word. If match case is false, the word with the correct
     * casing is returned. If no word is present, null is returned.
     *
     * @param word The word to look for.
     * @return Returns The word in the actual casing.
     * @throws IOException Thrown from the underlying implementations.
     */
    public String resolve(String word, boolean matchCase) throws IOException {
        // Open the ZIP file for the items.
        try (ZipFile zipFile = new ZipFile(file, Charsets.UTF_8)) {
            if (word.length() < prefixLength) {
                // If word to check for is shorter than the prefix, use shortnames table.
                ZipEntry entry = zipFile.getEntry(basePath + ".shortnames");

                // Open reader on the entry
                try (BufferedReader reader = new ByteSource() {
                    @Override
                    public InputStream openStream() throws IOException {
                        return zipFile.getInputStream(entry);
                    }
                }.asCharSource(Charsets.UTF_8).openBufferedStream()) {
                    // Read all lines, check if word is equal.
                    String line;
                    while ((line = reader.readLine()) != null)
                        if (matchCase) {
                            if (line.equals(word))
                                return line;
                        } else {
                            if (line.toLowerCase().equals(word.toLowerCase()))
                                return line;
                        }

                    return null;
                }
            } else {
                // Get the entry that starts with the prefix of the word
                ZipEntry entry = zipFile.getEntry(word.substring(0, prefixLength).toLowerCase());

                // No file with this prefix, so the word is not in the database.
                if (entry == null)
                    return null;

                // Open reader on the entry
                try (BufferedReader reader = new ByteSource() {
                    @Override
                    public InputStream openStream() throws IOException {
                        return zipFile.getInputStream(entry);
                    }
                }.asCharSource(Charsets.UTF_8).openBufferedStream()) {
                    // Read all lines, check if word is equal.
                    String line;
                    while ((line = reader.readLine()) != null)
                        if (matchCase) {
                            if (line.equals(word))
                                return line;
                        } else {
                            if (line.toLowerCase().equals(word.toLowerCase()))
                                return line;
                        }

                    return null;
                }
            }
        }
    }
}
