package eu.metatools.dbs;

import java.io.File;
import java.io.IOException;


public class Tests {
    public static void main(String[] args) throws IOException {

        StrDb strDb = new StrDb(new File("english_words_all.zip"), "english_words_all/", Config.prefixLength);

        System.out.println(strDb.resolve("prenewtonian"));
    }
}
