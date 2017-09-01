package eu.metatools.dbs;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.LineProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StrDb {
    private Map<String, Integer> index;

    /**
     * The byte source containing the prefixes.
     */
    private final ByteSource source;

    /**
     * The base path within the ZIP file.
     */
    private final String basePath;

    /**
     * The length of the prefix.
     */
    private final int prefixLength;

    /**
     * The charset to use for the database.
     */
    private final Charset charset;

    /**
     * Constructs the string DB.
     *
     * @param source       The byte source containing the prefixes.
     * @param basePath     The base path within the ZIP file.
     * @param prefixLength The length of the prefix.
     * @param charset      The charset to use for the database.
     */
    public StrDb(ByteSource source, String basePath, int prefixLength, Charset charset) {

        this.source = source;
        this.basePath = basePath;
        this.prefixLength = prefixLength;
        this.charset = charset;
    }

    public void index() throws IOException {
        index = new HashMap<>();

        // Can be in file, so open stream
        try (ZipInputStream stream = new ZipInputStream(source.openStream())) {
            ZipEntry entry;
            int i = 0;
            while ((entry = stream.getNextEntry()) != null) {
                String name = entry.getName().substring(basePath.length());
                index.put(name, i++);
            }
        }
    }

    private BufferedReader readerOn(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, charset));
    }

    /**
     * Provides all words to the consumer.
     *
     * @param consumer The consumer to feed with the words.
     * @throws IOException Thrown from the underlying implementations.
     */
    public void words(final Consumer<String> consumer) throws IOException {
        try (ZipInputStream stream = new ZipInputStream(source.openStream())) {
            while (stream.getNextEntry() != null) {
                BufferedReader reader = readerOn(stream);

                String line;
                while ((line = reader.readLine()) != null)
                    consumer.apply(line);
                ;
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
        try (ZipInputStream stream = new ZipInputStream(source.openStream())) {
            while (stream.getNextEntry() != null) {
                BufferedReader reader = readerOn(stream);

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!processor.processLine(line))
                        return processor.getResult();
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
        // Index if not yet indexed
        if (index == null)
            index();

        // get the prefix of the word
        String prefix = word.substring(0, prefixLength).toLowerCase();

        // Find the position of the prefix, if not found, it is not contained
        Integer position = index.get(prefix);
        if (position == null)
            return null;

        // Can be in file, so open stream
        try (ZipInputStream stream = new ZipInputStream(source.openStream())) {
            // Spool to the correct entry
            for (int i = 0; i <= position; i++)
                stream.getNextEntry();

            // Open reader on the position
            BufferedReader reader = readerOn(stream);

            // Find using rules
            String line;
            while ((line = reader.readLine()) != null)
                if (matchCase) {
                    if (line.equals(word))
                        return line;
                } else {
                    if (line.toLowerCase().equals(word.toLowerCase()))
                        return line;
                }

            // Not found, return null
            return null;
        }
    }
}
