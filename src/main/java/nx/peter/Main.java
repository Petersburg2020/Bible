package nx.peter;

import nx.peter.java.bible.Bible;
import nx.peter.java.bible.DARBYBible;
import nx.peter.java.bible.RVABible;

public class Main {
    public static void main(String[] args) {
        Bible bible = new DARBYBible();

        println(bible.getBook("Heb").toJson().getPrettyPrinter());

        // String store = "src/main/java/nx/peter/store/";
        // println(FileManager.writeFile(store + "sample.txt", "Welcome to Free Zone", false));

        /*bible.findVerses("Heb", "Jesus", 50, false, new Bible.OnSearchListener<>() {
            @Override
            public void onSearchInProgress(Bible.Result<Bible.Verse> result, int durationInSecs) {
                print(durationInSecs + " ");
            }

            @Override
            public void onSearchCompleted(Bible.Result<Bible.Verse> result, long durationInMillis) {
                println(durationInMillis);
                println(result.getAny().toJson().getPrettyPrinter());
            }
        });*/
    }

    public static void print(Object obj) {
        System.out.print(obj != null ? obj.toString() : null);
    }

    public static void println(Object obj) {
        System.out.println(obj != null ? obj.toString() : null);
    }

    public static void println() {
        println("");
    }

}