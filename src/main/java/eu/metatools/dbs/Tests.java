package eu.metatools.dbs;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.function.Predicate;


public class Tests {
    static int countFound = 0;

    static int countActual = 0;

    private static final Random random = new Random();

    public static String scramble(String s) {
        int n = random.nextInt(s.length());
        for (int i = 0; i < n; i++)
            s = scrambleOnce(s);
        return s;
    }

    public static String scrambleOnce(String s) {
        int n = random.nextInt(s.length());
        return s.substring(0, n) + s.substring(n + 1, s.length());
    }

    public static void main(String[] args) throws IOException {
        String[] tests = {

                "zoaea",
                "zoaeae",
                "zoaeal",
                "zoaeas",
                "zoamylin",
                "zoanthacea",
                "zoantharian",
                "zoantharians",
                "zoanthid",
                "zoanthids",
                "zoanthodeme",
                "zoanthodemes",
                "zoanthoid",
                "zoanthropic",
                "zoanthropy",
                "zoar",
                "zoarchaeologist",
                "zoarchaeologists",
                "zoarcid",
                "zoarcids",
                "zoaria",
                "zoarial",
                "zoarium",
                "zoars",
                "zobo",
                "zobos",
                "zocainone",
                "zocalo",
                "zocalos",
                "zocco",
                "zoccoli",
                "zoccolo",
                "zoccolos",
                "zoccos",
                "zocle",
                "zocles",
                "zodacite",
                "zodariid",
                "zodariids",
                "zodiac",
                "zodiacal",
                "zodiack",
                "zodiacks",
                "zodiacs",
                "zodico",
                "zoea",
                "zoeae",
                "zoeal",
                "zoeas",
                "zoechrome",
                "zoecia",
                "zoecial",
                "zoecium",
                "zoetic",
                "zoetrope",
                "zoetropes",
                "zoetropic",
                "zofenopril",
                "zofenoprilat",
                "zoficonazole",
                "zoftig",
                "zogist",
                "zogists",
                "zograf",
                "zograscope",
                "zograscopes",
                "zohar",
                "zoharism",
                "zoharist",
                "zoharists",
                "zoiatria",
                "zoiatric",
                "zoic",
                "zoiks",
                "zoilean",
                "zoili",
                "zoilism",
                "zoilus",
                "zoiluses",
                "zoinks",
                "zoisite",
                "zoisites",
                "zoisitic",
                "zoism",
                "zoist",
                "zoistic",
                "zoists",
                "zoite",
                "zoites",
                "zokor",
                "zokors",
                "zol",
                "zolaesque",
                "zolaism",
                "zolaist",
                "zolaistic",
                "zolaists",
                "zolaize",
                "zolaized",
                "zolaizes",
                "zolaizing",
                "zolamine",
                "zolantidine",
                "zolasartan",
                "zolazepam",
                "zoledronate",
                "zoledronates",
                "zoledronic",
                "zolenzepine",
                "zolertine",
                "zoletil",
                "zolian",
                "zolimidine",
                "zolimomab",
                "zoliprofen",
                "zollies",
                "zollverein",
                "zollvereins",
                "zolly",
                "zolmitriptan",
                "zoloft",
                "zoloperone",
                "zolotnik",
                "zolotniks",
                "zolpidem",
                "zols",
                "zoltan",
                "zomb",
                "zombi",
                "zombic",
                "zombie",
                "zombied",
                "zombiedom",
                "zombiefied",
                "zombiefies",
                "zombiefy",
                "zombiefying",
                "zombiehood",
                "zombieism",
                "zombiekind",
                "zombielike",
                "zombies",
                "zombiesat",
                "zombiesats",
                "zombiesque",
                "zombification",
                "zombifications",
                "zombified",
                "zombifies",
                "zombify",
                "zombifying",
                "zombiism",
                "zombis",
                "zombocalypse",
                "zomboid",
                "zomboruk",
                "zomboruks",
                "zombs",
                "zombyism",
                "zome",
                "zomebazam",
                "zomepirac",
                "zomes",
                "zomi",
                "zomotherapy",
                "zompire",
                "zompires",
                "zona",
                "zonadhesin",
                "zonae",
                "zonal",
                "zonalities",
                "zonality",
                "zonally",
                "zonampanel",
                "zonaras",
                "zonary",
                "zonas",
                "zonate",
                "zonated",
                "zonation",
                "zonations",
                "zonda",
                "zondas",
                "zone",
                "zoned",
                "zoneless",
                "zonelet",
                "zonelets",
                "zoner",
                "zoners",
                "zones"
        };

        String[] tests2 = new String[tests.length];
        for (int i = 0; i < tests.length; i++)
            tests2[i] = scramble(tests[i]);

        StrDb strDb = new StrDb(new File("english_words_all.zip"), "english_words_all/", 3);

        long bn = System.nanoTime();

        System.out.println(strDb.where(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.contains("oo");
            }
        }));

        long en = System.nanoTime();

        System.out.println("Total " + (en - bn));
        System.out.println("Per item " + ((en - bn) / (double) tests.length));
    }
}
