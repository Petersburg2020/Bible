package nx.peter;

import nx.peter.java.bible.Bible;
import nx.peter.java.bible.RVABible;

public class Main {
    public static void main(String[] args) {
        // Bible bible = new RVABible();

        // String store = "src/main/java/nx/peter/store/";
        // println(FileManager.writeFile(store + "sample.txt", "Welcome to Free Zone", false));
        /*
        bible.findChapters("Josué", new Bible.OnSearchListener<>() {
            @Override
            public void onSearchInProgress(Bible.Result<Bible.Chapter> result, int durationInSecs) {
                println(durationInSecs);
            }

            @Override
            public void onSearchCompleted(Bible.Result<Bible.Chapter> result, long durationInMillis) {
                println(durationInMillis);
                println(result.getAny().toJson().getArray("verses").getPrettyPrinter());
            }
        });*/
    }

    public static void println(Object obj) {
        System.out.println(obj != null ? obj.toString() : null);
    }

    public static void println() {
        println("");
    }

}