package eu.metatools.dbs;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class Tests {
    public static void main(String[] args) throws IOException {

        StrDb strDb = new StrDb(
                Files.asByteSource(new File("english_words_all.zip")),
                "english_words_all/",
                Config.prefixLength,
                StandardCharsets.UTF_8);

        System.out.println(strDb.contains("precip"));
        System.out.println(strDb.resolve("prenewtonian"));
        strDb.words(new Consumer<String>() {
            @Override
            public void apply(String item) {
                if (item.contains("oo"))
                    System.out.println(item);
            }
        });
    }
}
