package nx.peter.java.bible;

import nx.peter.java.json.JsonArray;
import nx.peter.java.json.reader.JsonObject;
import nx.peter.java.util.Random;
import nx.peter.java.util.Util;
import nx.peter.java.util.data.IData;
import nx.peter.java.util.data.Letters;
import nx.peter.java.util.data.Texts;
import nx.peter.java.util.data.Word;
import nx.peter.java.util.storage.FileManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Bible {
    Books getBooks();

    String getName();

    Verses getVerses();

    int getBookCount();

    int getVerseCount();

    Version getVersion();

    Chapters getChapters();

    int getChapterCount();

    int getTestamentCount();

    Texts getContent();

    Testaments getTestaments();

    Verse getVerse(CharSequence book, int chapter, int verse);

    Chapter getChapter(CharSequence book, int chapter);

    Book getBook(CharSequence book);

    Testament getTestament(Era era);

    void findChapters(CharSequence query, OnSearchListener<Chapter> listener);

    void findChapters(Era era, CharSequence query, OnSearchListener<Chapter> listener);

    void findChapters(CharSequence book, CharSequence query, OnSearchListener<Chapter> listener);

    void findVerses(CharSequence query, OnSearchListener<Verse> listener);

    void findVerses(Era era, CharSequence query, OnSearchListener<Verse> listener);

    void findVerses(CharSequence book, CharSequence query, OnSearchListener<Verse> listener);

    void findVerses(CharSequence book, int chapter, CharSequence query, OnSearchListener<Verse> listener);


    enum Era {
        New, Old
    }

    enum Version {
        ACV, AKJV, ASV, BBE, DARBY, DRA, RVA, YLT
    }

    interface Item<I extends Item> {
        int getIndex();

        Texts getContent();

        int getWordCount();

        JsonObject toJson();

        boolean equals(I item);

        boolean equalsIgnoreIndex(I item);
    }

    class Result<I extends Item<I>> extends Array<Result<I>, I> {
        protected CharSequence query, source;

        public Result(CharSequence query, CharSequence source) {
            this(query, source, new ArrayList<>());
        }

        public Result(CharSequence query, CharSequence source, List<I> items) {
            super(items);
            this.query = query;
            this.source = source;
        }

        public CharSequence getQuery() {
            return query;
        }

        public Texts getSource() {
            return new Texts(source);
        }
    }

    interface Testament extends Item<Testament> {
        String ERA = "testament", BOOKS = "books";

        Era getEra();

        Books getBooks();

        Verses getVerses();

        int getBookCount();

        int getVerseCount();

        int getChapterCount();

        Chapters getChapters();

        Book getBook(int book);

        boolean contains(Book book);

        boolean contains(Verse verse);

        boolean contains(Chapter chapter);

        boolean containsBook(CharSequence book);

        boolean containsVerse(CharSequence verse);

        Verse getVerse(CharSequence book, int chapter, int verse);

        Chapter getChapter(CharSequence book, int chapter);

        Book getBook(CharSequence name);

        Result<Chapter> findChapters(CharSequence query);

        Result<Chapter> findChapters(CharSequence book, CharSequence query);

        Result<Verse> findVerses(CharSequence query);

        Result<Verse> findVerses(CharSequence book, CharSequence query);

        Result<Verse> findVerses(CharSequence book, int chapter, CharSequence query);

        class Builder {
            protected Era era;
            protected List<Book> books;

            public Builder(Era era) {
                this(era, new ArrayList<>());
            }

            public Builder(Era era, List<Book> books) {
                this.era = era;
                this.books = books;
            }

            public Builder addBook(Book book) {
                if (book != null && !books.contains(book)) books.add(book);
                return this;
            }

            @Override
            public String toString() {
                return "[testament: " + era + ", books: [" + Util.toLines(Util.toList(books)) + "]]";
            }

            public Testament build() {
                return new Testament() {
                    @Override
                    public Result<Verse> findVerses(CharSequence query) {
                        List<Verse> verses = new ArrayList<>();
                        for (Chapter chapter : getChapters())
                            verses.addAll(chapter.findVerses(query).toList());
                        return new Result<>(query, getContent(), verses);
                    }

                    @Override
                    public Result<Verse> findVerses(CharSequence book, CharSequence query) {
                        Book b = getBook(book);
                        return b != null ? b.findVerses(query) : new Result<>(query, "");
                    }

                    @Override
                    public Result<Verse> findVerses(CharSequence book, int chapter, CharSequence query) {
                        Chapter c = getChapter(book, chapter);
                        return c != null ? c.findVerses(query) : new Result<>(query, "");
                    }

                    @Override
                    public Texts getContent() {
                        Texts content = new Texts(getEra() + " Testament");
                        for (Book book : books)
                            content.append("\n").append(book.getContent());
                        return content;
                    }

                    @Override
                    public int getWordCount() {
                        return getContent().getWordsOnly().size();
                    }

                    @Override
                    public JsonObject toJson() {
                        nx.peter.java.json.JsonObject object = new nx.peter.java.json.JsonObject();
                        object.add(ERA, era);

                        // Add books
                        JsonArray booksArr = new JsonArray();
                        for (Book book : books)
                            booksArr.add(book.toJson());
                        object.add(BOOKS, booksArr);
                        return object;
                    }

                    @Override
                    public int getIndex() {
                        return era.equals(Era.Old) ? 1 : 2;
                    }

                    @Override
                    public boolean equals(Testament testament) {
                        return equalsIgnoreIndex(testament) && testament.getIndex() == getIndex();
                    }

                    @Override
                    public boolean equalsIgnoreIndex(Testament testament) {
                        return testament != null && testament.getBooks().equals(getBooks());
                    }

                    @Override
                    public Era getEra() {
                        return era;
                    }

                    @Override
                    public Books getBooks() {
                        return new Books(books);
                    }

                    @Override
                    public Verses getVerses() {
                        List<Verse> verses = new ArrayList<>();
                        for (Chapter chapter : getChapters())
                            verses.addAll(chapter.getVerses().toList());
                        return new Verses(verses);
                    }

                    @Override
                    public int getBookCount() {
                        return books.size();
                    }

                    @Override
                    public int getVerseCount() {
                        return getVerses().size();
                    }

                    @Override
                    public int getChapterCount() {
                        return getChapters().size();
                    }

                    @Override
                    public Chapters getChapters() {
                        List<Chapter> chapters = new ArrayList<>();
                        for (Book book : books)
                            chapters.addAll(book.getChapters().toList());
                        return new Chapters(chapters);
                    }

                    @Override
                    public Book getBook(int book) {
                        return getBooks().get(book);
                    }

                    @Override
                    public boolean contains(Book book) {
                        return getBooks().contains(book);
                    }

                    @Override
                    public boolean contains(Verse verse) {
                        for (Book book : getBooks())
                            if (book.contains(verse)) return true;
                        return false;
                    }

                    @Override
                    public boolean contains(Chapter chapter) {
                        for (Book book : getBooks())
                            if (book.contains(chapter)) return true;
                        return false;
                    }

                    @Override
                    public boolean containsBook(CharSequence book) {
                        return getBooks().contains(book);
                    }

                    @Override
                    public boolean containsVerse(CharSequence verse) {
                        for (Book book : books)
                            if (book.contains(verse)) return true;
                        return false;
                    }

                    @Override
                    public Verse getVerse(CharSequence book, int chapter, int verse) {
                        Chapter c = getChapter(book, chapter);
                        return c != null ? c.getVerse(verse) : null;
                    }

                    @Override
                    public Chapter getChapter(CharSequence book, int chapter) {
                        Book b = getBook(book);
                        return b != null ? b.getChapter(chapter) : null;
                    }

                    @Override
                    public String toString() {
                        return getContent().get();
                    }

                    @Override
                    public Book getBook(CharSequence name) {
                        for (Book book : books)
                            if (book.getTitle().contentEquals(name) || book.getAbbreviation().contentEquals(name))
                                return book;
                        return null;
                    }

                    @Override
                    public Result<Chapter> findChapters(CharSequence query) {
                        List<Chapter> chapters = new ArrayList<>();
                        for (Book book : books)
                            for (Chapter chapter : book.getChapters())
                                if (chapter.contains(query)) chapters.add(chapter);
                        return new Result<>(query, getContent(), chapters);
                    }

                    @Override
                    public Result<Chapter> findChapters(CharSequence book, CharSequence query) {
                        Book b = getBook(book);
                        return b != null ? b.findChapters(query) : new Result<>(query, "");
                    }
                };
            }
        }
    }

    interface Book extends Item<Book> {
        String TITLE = "title", ERA = "testament", INDEX = "book", ABBR = "abbreviation", CHAPTERS = "chapters";


        String getTitle();

        Verses getVerses();

        Era getTestament();

        int getVerseCount();

        int getChapterCount();

        Chapters getChapters();

        String getAbbreviation();

        Verse getVerse(int verse);

        Chapter getChapter(int chapter);

        boolean contains(Chapter chapter);

        boolean contains(Verse verse);

        boolean contains(CharSequence verse);

        Result<Chapter> findChapters(CharSequence query);

        Result<Verse> findVerses(CharSequence query);

        Result<Verse> findVerses(int chapter, CharSequence query);

        class Builder {
            protected int index;
            protected final String title;
            protected String abbr;
            protected List<Chapter> chapters;
            protected Era era;

            public Builder(CharSequence title) {
                this(title, "", new ArrayList<>());
            }

            public Builder(CharSequence title, CharSequence abbr, List<Chapter> chapters) {
                this.title = title.toString();
                this.abbr = abbr.toString();
                this.chapters = new ArrayList<>();
                for (Chapter c : chapters)
                    if (c != null) this.chapters.add(c);
                index = 0;
            }

            public Builder addChapter(Chapter chapter) {
                if (chapter != null && !new Chapters(chapters).contains(chapter)) chapters.add(chapter);
                return this;
            }

            public Builder setAbbr(CharSequence abbr) {
                this.abbr = abbr != null ? abbr.toString() : "";
                return this;
            }

            public Builder setEra(Era era) {
                this.era = era;
                return this;
            }

            public Builder setIndex(int index) {
                this.index = index;
                return this;
            }

            @Override
            public String toString() {
                return "[book: " + title + ", chapters: [" + Util.toLines(Util.toList(chapters)) + "]]";
            }

            public Book build() {
                return new Book() {
                    @Override
                    public Result<Verse> findVerses(CharSequence query) {
                        List<Verse> verses = new ArrayList<>();
                        for (Chapter chapter : getChapters())
                            verses.addAll(chapter.findVerses(query).toList());
                        return new Result<>(query, getContent(), verses);
                    }

                    @Override
                    public Result<Verse> findVerses(int chapter, CharSequence query) {
                        Chapter c = getChapter(chapter);
                        return c != null ? c.findVerses(query) : new Result<>(query, "");
                    }

                    @Override
                    public Texts getContent() {
                        Texts content = new Texts(getTitle());
                        for (Chapter chapter : chapters)
                            content.append("\n").append(chapter.getContent());
                        return content;
                    }

                    @Override
                    public int getWordCount() {
                        return getContent().getWordsOnly().size();
                    }

                    @Override
                    public JsonObject toJson() {
                        nx.peter.java.json.JsonObject object = new nx.peter.java.json.JsonObject();
                        object.add(TITLE, title);
                        object.add(ABBR, abbr);
                        object.add(ERA, era);
                        object.add(INDEX, index);

                        // Add Chapters
                        JsonArray chaptersArr = new JsonArray();
                        for (Chapter chapter : chapters)
                            chaptersArr.add(chapter.toJson());
                        object.add(CHAPTERS, chaptersArr);
                        return object;
                    }

                    @Override
                    public Era getTestament() {
                        return era;
                    }

                    @Override
                    public boolean equalsIgnoreIndex(Book book) {
                        return book != null && book.getChapters().toList().equals(chapters);
                    }

                    @Override
                    public boolean equals(Book book) {
                        return equalsIgnoreIndex(book) && book.getIndex() == index;
                    }

                    @Override
                    public int getIndex() {
                        return index;
                    }

                    @Override
                    public Verses getVerses() {
                        List<Verse> verses = new ArrayList<>();
                        for (Chapter c : chapters)
                            verses.addAll(c.getVerses().toList());
                        return new Verses(verses);
                    }

                    @Override
                    public int getVerseCount() {
                        return getVerses().size();
                    }

                    @Override
                    public int getChapterCount() {
                        return getChapters().size();
                    }

                    @Override
                    public Verse getVerse(int verse) {
                        return getVerses().get(verse);
                    }

                    @Override
                    public Chapter getChapter(int chapter) {
                        return getChapters().get(chapter);
                    }

                    @Override
                    public String getAbbreviation() {
                        return abbr;
                    }

                    @Override
                    public String getTitle() {
                        return title;
                    }

                    @Override
                    public Chapters getChapters() {
                        return new Chapters(chapters);
                    }

                    @Override
                    public String toString() {
                        return getContent().get();
                    }

                    @Override
                    public boolean contains(Chapter chapter) {
                        return getChapters().contains(chapter);
                    }

                    @Override
                    public boolean contains(Verse verse) {
                        return getChapters().contains(verse);
                    }

                    @Override
                    public boolean contains(CharSequence verse) {
                        return getChapters().contains(verse);
                    }

                    @Override
                    public Result<Chapter> findChapters(CharSequence query) {
                        List<Chapter> chapters = new ArrayList<>();
                        for (Chapter chapter : getChapters())
                            if (chapter.contains(query)) chapters.add(chapter);
                        return new Result<>(query, getContent(), chapters);
                    }
                };
            }
        }
    }

    interface Chapter extends Item<Chapter> {
        String CHAPTER = "chapter", VERSES = "verses", BOOK = "book", ERA = "era";

        Verse getVerse(int verse);

        Verses getVerses();

        String getBook();

        Era getTestament();

        int getVerseCount();

        boolean contains(Verse verse);

        boolean contains(CharSequence verse);

        Result<Verse> findVerses(CharSequence query);

        class Builder {
            protected Era era;
            protected int index;
            protected String book;
            protected List<Verse> verses;

            public Builder(int chapter) {
                this(chapter, new ArrayList<>());
            }

            public Builder(Chapter chapter) {
                index = chapter.getIndex();
                book = chapter.getBook();
                era = chapter.getTestament();
                verses = chapter.getVerses().toList();
            }

            public Builder(int chapter, List<Verse> verses) {
                this.index = chapter;
                this.verses = new ArrayList<>();
                for (Verse v : verses)
                    if (v.isValid() && !contains(v)) this.verses.add(v);
            }

            public Builder setBook(String book) {
                this.book = book;
                return this;
            }

            public Builder setEra(Era era) {
                this.era = era;
                return this;
            }

            public Builder addVerse(Verse verse) {
                if (verse != null) verses.add(verse);
                return this;
            }

            @Override
            public String toString() {
                return "[chapter: " + index + ", verses: [" + Util.toLines(Util.toList(verses)) + "]]";
            }

            protected boolean contains(Verse verse) {
                return new Verses(verses).contains(verse);
            }

            public Chapter build() {
                return new Chapter() {

                    @Override
                    public Texts getContent() {
                        Texts content = new Texts("Chapter " + index);
                        for (Verse verse : verses)
                            content.append("\n").append(verse.getContent());
                        return content;
                    }

                    @Override
                    public int getWordCount() {
                        return getContent().getWordsOnly().size();
                    }

                    @Override
                    public Result<Verse> findVerses(CharSequence query) {
                        List<Verse> verses = new ArrayList<>();
                        for (Verse verse : getVerses())
                            if (verse.contains(query)) verses.add(verse);
                        return new Result<>(query, getContent(), verses);
                    }


                    @Override
                    public Era getTestament() {
                        return era;
                    }

                    @Override
                    public String getBook() {
                        return book;
                    }

                    @Override
                    public JsonObject toJson() {
                        nx.peter.java.json.JsonObject object = new nx.peter.java.json.JsonObject();
                        object.add(CHAPTER, index)
                                .add(ERA, era)
                                .add(BOOK, book);

                        // Add Verses
                        nx.peter.java.json.JsonArray array = new nx.peter.java.json.JsonArray();
                        for (Verse verse : verses)
                            array.add(verse.toJson());
                        object.add(VERSES, array);
                        return object;
                    }

                    @Override
                    public boolean equalsIgnoreIndex(Chapter chapter) {
                        return chapter != null && chapter.getVerses().equals(getVerses());
                    }

                    @Override
                    public int getIndex() {
                        return index;
                    }

                    @Override
                    public Verse getVerse(int verse) {
                        return getVerses().get(verse);
                    }

                    @Override
                    public Verses getVerses() {
                        return new Verses(verses);
                    }

                    @Override
                    public int getVerseCount() {
                        return verses.size();
                    }

                    @Override
                    public boolean contains(Verse verse) {
                        return new Verses(verses).contains(verse);
                    }

                    @Override
                    public boolean contains(CharSequence verse) {
                        return new Verses(verses).contains(verse);
                    }

                    @Override
                    public boolean equals(Chapter chapter) {
                        return chapter != null && chapter.getVerses().equals(getVerses());
                    }

                    @Override
                    public String toString() {
                        return getContent().get();
                    }
                };
            }
        }
    }

    interface Verse extends Item<Verse>, CharSequence {
        String BOOK = "book", CHAPTER = "chapter", TESTAMENT = "testament", VERSE = "verse", CONTENT = "content";

        boolean isValid();

        boolean isEmpty();

        boolean isNotEmpty();

        String getBook();

        String getBookAbbr();

        int getChapter();

        Era getTestament();

        boolean contains(IData<?> query);

        boolean contains(Object query);

        boolean contains(CharSequence query);

        boolean equals(CharSequence verse);


        class Builder {
            private final String content;
            private String book, abbr;
            private final int verse;
            private int chapter;
            private Era era;

            public Builder(Verse verse) {
                content = verse.getContent().get();
                this.verse = verse.getIndex();
                chapter = verse.getChapter();
                era = verse.getTestament();
                book = verse.getBook();
            }

            public Builder(int verse, CharSequence content) {
                this.verse = verse;
                this.content = content.toString();
            }

            public Builder setAbbr(String abbr) {
                this.abbr = abbr;
                return this;
            }

            public Builder setBook(String book) {
                this.book = book;
                return this;
            }

            public Builder setChapter(int chapter) {
                this.chapter = chapter;
                return this;
            }

            public Builder setEra(Era era) {
                this.era = era;
                return this;
            }

            @Override
            public String toString() {
                return "{" + abbr + " " + chapter + ":" + verse + " " + content + "}";
            }

            public Verse build() {
                return new Verse() {

                    @Override
                    public int getWordCount() {
                        return getContent().getWordsOnly().size();
                    }

                    @Override
                    public JsonObject toJson() {
                        nx.peter.java.json.JsonObject object = new nx.peter.java.json.JsonObject();
                        object.add(TESTAMENT, era)
                                .add(BOOK, book)
                                .add(CHAPTER, chapter)
                                .add(VERSE, verse)
                                .add(CONTENT, content);
                        return object;
                    }

                    @Override
                    public boolean contains(IData<?> query) {
                        return query != null && contains(query.get());
                    }

                    @Override
                    public boolean contains(Object query) {
                        return query != null && contains(query.toString());
                    }

                    @Override
                    public boolean contains(CharSequence query) {
                        return query != null && content.contains(query);
                    }

                    @Override
                    public Era getTestament() {
                        return era;
                    }

                    @Override
                    public String getBook() {
                        return book;
                    }

                    @Override
                    public String getBookAbbr() {
                        return abbr;
                    }

                    @Override
                    public int getChapter() {
                        return chapter;
                    }

                    @Override
                    public int getIndex() {
                        return verse;
                    }

                    @Override
                    public boolean equalsIgnoreIndex(Verse verse) {
                        return verse != null && verse.getContent().equalsIgnoreType(getContent());
                    }

                    @Override
                    public IntStream chars() {
                        return content.chars();
                    }

                    @Override
                    public IntStream codePoints() {
                        return content.codePoints();
                    }

                    @Override
                    public char charAt(int index) {
                        return content.charAt(index);
                    }

                    @Override
                    public boolean isValid() {
                        return verse > 0 && isNotEmpty();
                    }

                    @Override
                    public boolean isEmpty() {
                        return content.isEmpty();
                    }

                    @Override
                    public boolean isNotEmpty() {
                        return !isEmpty();
                    }

                    @Override
                    public boolean equals(CharSequence verse) {
                        return content.contentEquals(verse);
                    }

                    @Override
                    public boolean equals(Verse verse) {
                        return equalsIgnoreIndex(verse) && verse.getIndex() == getIndex();
                    }

                    @Override
                    public CharSequence subSequence(int start, int end) {
                        return content.subSequence(start, end);
                    }

                    @Override
                    public Texts getContent() {
                        return new Texts(book + " " + chapter + ":" + verse + ". " + content);
                    }

                    @Override
                    public int length() {
                        return content.length();
                    }

                    @Override
                    public String toString() {
                        return getContent().get();
                    }
                };
            }
        }

    }


    class Testaments implements Iterable<Testament> {
        protected Testament tNew, tOld;

        public Testaments(Testament tOld, Testament tNew) {
            this.tOld = tOld;
            this.tNew = tNew;
        }

        public int size() {
            return toList().size();
        }

        public List<Testament> toList() {
            List<Testament> list = new ArrayList<>();
            if (tOld != null) list.add(tOld);
            if (tNew != null) list.add(tNew);
            return list;
        }

        public boolean contains(Era era) {
            return get(era) != null;
        }

        public Testament get(Era era) {
            return era != null ? era.equals(Era.Old) ? tOld : tNew : null;
        }

        public boolean equals(Testaments another) {
            return another != null && another.toList().equals(toList());
        }

        @Override
        public String toString() {
            return new Texts(toList().stream().map(Testament::toString).collect(Collectors.toList()).toString()).remove("[").remove("]").get();
        }

        @Override
        public Iterator<Testament> iterator() {
            return toList().iterator();
        }
    }

    class Books extends Array<Books, Book> {
        public Books(List<Book> books) {
            super(books);
        }

        public Book get(CharSequence title) {
            for (Book b : items)
                if (b.getTitle().toLowerCase().contentEquals(title.toString().toLowerCase()) || b.getAbbreviation().toLowerCase().contentEquals(title.toString().toLowerCase())) return b;
            return null;
        }

        public boolean contains(CharSequence title) {
            return get(title) != null;
        }
    }

    class Chapters extends Array<Chapters, Chapter> {
        public Chapters(List<Chapter> chapters) {
            super(chapters);
        }

        public Chapter get(Verse verse) {
            for (Chapter c : items)
                if (c.contains(verse)) return c;
            return null;
        }

        public Chapter get(CharSequence verse) {
            for (Chapter c : items)
                if (c.contains(verse)) return c;
            return null;
        }

        public boolean contains(Verse verse) {
            return get(verse) != null;
        }

        public boolean contains(CharSequence verse) {
            return get(verse) != null;
        }
    }

    class Verses extends Array<Verses, Verse> {
        public Verses(List<Verse> verses) {
            super(verses);
        }

        public boolean contains(CharSequence query) {
            if (query != null) for (Verse v : items)
                if (v.equals(query) || v.contains(query)) return true;
            return false;
        }
    }


    abstract class Array<A extends Array<A, I>, I extends Item<I>> implements Iterable<I> {
        protected List<I> items;

        public Array(List<I> items) {
            this.items = items != null ? items : new ArrayList<>();
        }

        @SafeVarargs
        public Array(I... items) {
            this(Arrays.asList(items));
        }

        public Array() {
            this(new ArrayList<I>());
        }

        public I get(int index) {
            /*if (index > 0 && index <= size())
                for (I item : items)
                    if (item.getIndex() == index) return item;*/
            return index > 0 && index <= size() ? items.get(index - 1) : null;
        }

        public I getFirst() {
            return get(1);
        }

        public I getAny() {
            return get(isNotEmpty() ? Random.nextInt(1, size()) : 0);
        }

        public I getLast() {
            return get(size());
        }

        public int size() {
            return items.size();
        }

        public int indexOf(I item) {
            for (I i : items)
                if (i.equalsIgnoreIndex(item)) return i.getIndex();
            return 0;
        }

        public boolean contains(I item) {
            return items.contains(item);
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

        public boolean isNotEmpty() {
            return !isEmpty();
        }

        public boolean equals(A array) {
            return array != null && array.items.equals(items);
        }

        public Stream<I> stream() {
            return items.stream();
        }

        public void sort(Comparator<I> comparator) {
            items.sort(comparator);
        }

        public List<I> toList() {
            return items;
        }

        @Override
        public Iterator<I> iterator() {
            return items.iterator();
        }

        @Override
        public Spliterator<I> spliterator() {
            return items.spliterator();
        }

        @Override
        public String toString() {
            return new Texts(items.toString()).remove("[", 0).remove("]", size() - 2).get();
        }
    }


    abstract class IBible implements Bible {
        protected Testament tOld, tNew;
        protected Version version;
        protected String name;

        public IBible(Version version) {
            this.version = version;
            init();
        }

        protected void init() {
            String path = "src/main/java/nx/peter/store/bible/" + version.toString() + ".txt";
            List<String> lines = FileManager.readLines(path);

            name = new Texts(lines.get(0)).subLetters(0, lines.get(0).indexOf("(") - 1).get();
            System.out.println("Lines: " + lines.size());

            Testament.Builder testament = null;
            Book.Builder book = null;
            Chapter.Builder chapter = null;

            int bookIndex = 0, chapterIndex = 0, index = 0;

            for (String line : lines) {
                Texts texts = new Texts(line);
                if (index > 0) if (texts.isNotEmpty()) {
                    Letters.Words words = texts.getWords();
                    // Main.println(words);
                    if (words.size() == 1 || texts.startsWith("Revelation ") || texts.startsWith("Song of ") || texts.startsWith("I ") || texts.startsWith("II") || texts.startsWith("III") || (words.size() == 2 && (texts.startsWith("1") || texts.startsWith("2") || texts.startsWith("3"))) ) {
                        if (words.getFirst().contentEquals("Genesis") || words.getFirst().contentEquals("Génesis")) testament = new Testament.Builder(Era.Old);
                        else if (words.getFirst().contentEquals("Matthew")) {
                            if (testament != null) tOld = testament.build();
                            testament = new Testament.Builder(Era.New);
                        }

                        if (book != null) testament.addBook(book.build());

                        bookIndex++;
                        book = new Book.Builder(texts.get());
                        assert testament != null;
                        book.setEra(testament.era);
                        book.setIndex(bookIndex);
                        // chapterIndex = 1;
                        // chapter = new Chapter.Builder(1);
                        // System.out.println("Book: \"" + book.title + "\"");
                    } else {
                        Word first = words.getFirst();
                        String bookAbbr = first.substring(0, first.indexOf('.'));
                        if (book != null) {
                            book.setAbbr(bookAbbr);
                            // Main.println("\n" + book.title);
                            // Letters.Numbers numbers = first.extractIntegers();
                            Letters.NumbersCount count = first.subLetters(first.indexOf('.') + 1).getNumbersCount();
                            // Main.println(count + "\n");
                            // Main.println("First: \"" + first.get() + "\"");
                            int temp = count.getData(1).getInteger();
                            // Main.println("Chapter: " + temp);
                            if (temp != chapterIndex || (chapter != null && !chapter.book.contentEquals(book.title))) {
                                if (chapter != null) book.addChapter(chapter.build());
                                chapter = new Chapter.Builder(temp);
                                chapter.setBook(book.title);
                                chapter.setEra(book.era);
                                chapterIndex = temp;
                            }
                            temp = count.getData(2).getInteger();
                            // Main.println("Verse: " + temp);
                            String content = texts.subLetters(" ", 0).trim().get();
                            Verse.Builder verse = new Verse.Builder(temp, content);
                            verse.setBook(book.title);
                            verse.setChapter(chapterIndex);
                            verse.setEra(book.era);
                            verse.setAbbr(bookAbbr);

                            // Main.println(verse);
                            assert chapter != null;
                            chapter.addVerse(verse.build());
                        }
                    }
                }
                index++;
            }

            if (book != null && chapter != null) {
                book.addChapter(chapter.build());
                // Main.println(chapter);
                testament.addBook(book.build());
            }

            if (testament != null) if (testament.era.equals(Era.Old)) tOld = testament.build();
            else tNew = testament.build();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Books getBooks() {
            List<Book> books = new ArrayList<>();
            if (tOld != null) books.addAll(tOld.getBooks().toList());
            if (tNew != null) books.addAll(tNew.getBooks().toList());
            return new Books(books);
        }

        @Override
        public Verses getVerses() {
            List<Verse> verses = new ArrayList<>();
            if (tOld != null) verses.addAll(tOld.getVerses().toList());
            if (tNew != null) verses.addAll(tNew.getVerses().toList());
            return new Verses(verses);
        }

        @Override
        public int getBookCount() {
            return getBooks().size();
        }

        @Override
        public int getVerseCount() {
            return getVerses().size();
        }

        @Override
        public Version getVersion() {
            return version;
        }

        @Override
        public Chapters getChapters() {
            List<Chapter> chapters = new ArrayList<>();
            if (tOld != null) chapters.addAll(tOld.getChapters().toList());
            if (tNew != null) chapters.addAll(tNew.getChapters().toList());
            return new Chapters(chapters);
        }

        @Override
        public int getChapterCount() {
            return getChapters().size();
        }

        @Override
        public int getTestamentCount() {
            return getTestaments().size();
        }

        @Override
        public Verse getVerse(CharSequence book, int chapter, int verse) {
            Chapter c = getChapter(book, chapter);
            return c != null ? c.getVerse(verse) : null;
        }

        @Override
        public Chapter getChapter(CharSequence book, int chapter) {
            Book b = getBook(book);
            return b != null ? b.getChapter(chapter) : null;
        }

        @Override
        public Texts getContent() {
            Texts texts = new Texts();
            if (tOld != null) texts.append(tOld.getContent()).append("\n");
            if (tNew != null) texts.append(tNew.getContent());
            return texts;
        }

        @Override
        public Book getBook(CharSequence name) {
            if (tOld != null && tOld.containsBook(name)) return tOld.getBook(name);
            if (tNew != null && tNew.containsBook(name)) return tNew.getBook(name);
            return null;
        }

        @Override
        public Testament getTestament(Era era) {
            return getTestaments().get(era);
        }

        @Override
        public Testaments getTestaments() {
            return new Testaments(tOld, tNew);
        }

        @Override
        public void findChapters(CharSequence query, OnSearchListener<Chapter> listener) {
            List<Chapter> chapters = new ArrayList<>();
            Thread finder = new Thread(() -> findChapters(query, chapters));
            finder.start();
            AtomicLong start = new AtomicLong(System.currentTimeMillis());
            final int[] counter = {0};

            new Thread(() -> {
                while (finder.getState().equals(Thread.State.RUNNABLE)) {
                    long current = System.currentTimeMillis();
                    //
                    if ((current - start.get()) % 1000 == 0 && !finder.getState().equals(Thread.State.TERMINATED)) {
                        int duration = (int) ((current - start.get()) / 1000);
                        if (duration - counter[0] >= 1) {
                            listener.onSearchInProgress(new Result<>(query, getContent(), chapters), duration);
                            counter[0] = duration;
                        }
                    }

                    if (finder.getState().equals(Thread.State.TERMINATED)) {
                        listener.onSearchCompleted(new Result<>(query, getContent(), chapters), System.currentTimeMillis() - start.get());
                        break;
                    }
                }
            }).start();
        }

        @Override
        public void findChapters(Era era, CharSequence query, OnSearchListener<Chapter> listener) {
            List<Chapter> chapters = new ArrayList<>();
            Thread finder = new Thread(() -> findChapters(era, query, chapters));
            finder.start();
            AtomicLong start = new AtomicLong(System.currentTimeMillis());
            final int[] counter = {0};

            new Thread(() -> {
                while (finder.getState().equals(Thread.State.RUNNABLE)) {
                    long current = System.currentTimeMillis();
                    //
                    if ((current - start.get()) % 1000 == 0 && !finder.getState().equals(Thread.State.TERMINATED)) {
                        int duration = (int) ((current - start.get()) / 1000);
                        if (duration - counter[0] >= 1) {
                            listener.onSearchInProgress(new Result<>(query, getContent(), chapters), duration);
                            counter[0] = duration;
                        }
                    }

                    if (finder.getState().equals(Thread.State.TERMINATED)) {
                        listener.onSearchCompleted(new Result<>(query, getContent(), chapters), System.currentTimeMillis() - start.get());
                        break;
                    }
                }
            }).start();
        }

        @Override
        public void findChapters(CharSequence book, CharSequence query, OnSearchListener<Chapter> listener) {
            List<Chapter> chapters = new ArrayList<>();
            Thread finder = new Thread(() -> findChapters(book, query, chapters));
            finder.start();
            AtomicLong start = new AtomicLong(System.currentTimeMillis());
            final int[] counter = {0};

            new Thread(() -> {
                while (finder.getState().equals(Thread.State.RUNNABLE)) {
                    long current = System.currentTimeMillis();
                    //
                    if ((current - start.get()) % 1000 == 0 && !finder.getState().equals(Thread.State.TERMINATED)) {
                        int duration = (int) ((current - start.get()) / 1000);
                        if (duration - counter[0] >= 1) {
                            listener.onSearchInProgress(new Result<>(query, getContent(), chapters), duration);
                            counter[0] = duration;
                        }
                    }

                    if (finder.getState().equals(Thread.State.TERMINATED)) {
                        listener.onSearchCompleted(new Result<>(query, getContent(), chapters), System.currentTimeMillis() - start.get());
                        break;
                    }
                }
            }).start();
        }

        protected void findChapters(CharSequence query, List<Chapter> chapters) {
            if (tOld != null) chapters.addAll(tOld.findChapters(query).toList());
            if (tNew != null) chapters.addAll(tNew.findChapters(query).toList());
        }

        protected void findChapters(Era era, CharSequence query, List<Chapter> chapters) {
            if (tOld != null && tOld.getEra().equals(era)) chapters.addAll(tOld.findChapters(query).toList());
            else if (tNew != null && tNew.getEra().equals(era)) chapters.addAll(tNew.findChapters(query).toList());
        }

        protected void findChapters(CharSequence book, CharSequence query, List<Chapter> chapters) {
            if (tOld != null) chapters.addAll(tOld.findChapters(book, query).toList());
            if (tNew != null) chapters.addAll(tNew.findChapters(book, query).toList());
        }


        @Override
        public void findVerses(CharSequence query, OnSearchListener<Verse> listener) {
            List<Verse> verses = new ArrayList<>();
            Thread finder = new Thread(() -> findVerses(query, verses));
            finder.start();
            AtomicLong start = new AtomicLong(System.currentTimeMillis());
            final int[] counter = {0};

            new Thread(() -> {
                while (finder.getState().equals(Thread.State.RUNNABLE)) {
                    long current = System.currentTimeMillis();
                    //
                    if ((current - start.get()) % 1000 == 0 && !finder.getState().equals(Thread.State.TERMINATED)) {
                        int duration = (int) ((current - start.get()) / 1000);
                        if (duration - counter[0] >= 1) {
                            listener.onSearchInProgress(new Result<>(query, getContent(), verses), duration);
                            counter[0] = duration;
                        }
                    }

                    if (finder.getState().equals(Thread.State.TERMINATED)) {
                        listener.onSearchCompleted(new Result<>(query, getContent(), verses), System.currentTimeMillis() - start.get());
                        break;
                    }
                }
            }).start();
        }

        @Override
        public void findVerses(Era era, CharSequence query, OnSearchListener<Verse> listener) {
            List<Verse> verses = new ArrayList<>();
            Thread finder = new Thread(() -> findVerses(era, query, verses));
            finder.start();
            AtomicLong start = new AtomicLong(System.currentTimeMillis());
            final int[] counter = {0};

            new Thread(() -> {
                while (finder.getState().equals(Thread.State.RUNNABLE)) {
                    long current = System.currentTimeMillis();
                    //
                    if ((current - start.get()) % 1000 == 0 && !finder.getState().equals(Thread.State.TERMINATED)) {
                        int duration = (int) ((current - start.get()) / 1000);
                        if (duration - counter[0] >= 1) {
                            listener.onSearchInProgress(new Result<>(query, getContent(), verses), duration);
                            counter[0] = duration;
                        }
                    }

                    if (finder.getState().equals(Thread.State.TERMINATED)) {
                        listener.onSearchCompleted(new Result<>(query, getContent(), verses), System.currentTimeMillis() - start.get());
                        break;
                    }
                }
            }).start();
        }

        @Override
        public void findVerses(CharSequence book, CharSequence query, OnSearchListener<Verse> listener) {
            List<Verse> verses = new ArrayList<>();
            Thread finder = new Thread(() -> findVerses(book, query, verses));
            finder.start();
            AtomicLong start = new AtomicLong(System.currentTimeMillis());
            final int[] counter = {0};

            new Thread(() -> {
                while (finder.getState().equals(Thread.State.RUNNABLE)) {
                    long current = System.currentTimeMillis();
                    //
                    if ((current - start.get()) % 1000 == 0 && !finder.getState().equals(Thread.State.TERMINATED)) {
                        int duration = (int) ((current - start.get()) / 1000);
                        if (duration - counter[0] >= 1) {
                            listener.onSearchInProgress(new Result<>(query, getContent(), verses), duration);
                            counter[0] = duration;
                        }
                    }

                    if (finder.getState().equals(Thread.State.TERMINATED)) {
                        listener.onSearchCompleted(new Result<>(query, getContent(), verses), System.currentTimeMillis() - start.get());
                        break;
                    }
                }
            }).start();
        }

        @Override
        public void findVerses(CharSequence book, int chapter, CharSequence query, OnSearchListener<Verse> listener) {
            List<Verse> verses = new ArrayList<>();
            Thread finder = new Thread(() -> findVerses(book, chapter, query, verses));
            finder.start();
            AtomicLong start = new AtomicLong(System.currentTimeMillis());
            final int[] counter = {0};

            new Thread(() -> {
                while (finder.getState().equals(Thread.State.RUNNABLE)) {
                    long current = System.currentTimeMillis();
                    //
                    if ((current - start.get()) % 1000 == 0 && !finder.getState().equals(Thread.State.TERMINATED)) {
                        int duration = (int) ((current - start.get()) / 1000);
                        if (duration - counter[0] >= 1) {
                            listener.onSearchInProgress(new Result<>(query, getContent(), verses), duration);
                            counter[0] = duration;
                        }
                    }

                    if (finder.getState().equals(Thread.State.TERMINATED)) {
                        listener.onSearchCompleted(new Result<>(query, getContent(), verses), System.currentTimeMillis() - start.get());
                        break;
                    }
                }
            }).start();
        }

        protected void findVerses(CharSequence query, List<Verse> result) {
            if (tOld != null) result.addAll(tOld.findVerses(query).toList());
            if (tNew != null) result.addAll(tNew.findVerses(query).toList());
        }

        protected void findVerses(Era testament, CharSequence query, List<Verse> result) {
            Testament t = getTestament(testament);
            result.addAll(t != null ? t.findVerses(query).toList() : new ArrayList<>());
        }

        protected void findVerses(CharSequence book, CharSequence query, List<Verse> result) {
            if (tOld != null) result.addAll(tOld.findVerses(book, query).toList());
            if (tNew != null) result.addAll(tNew.findVerses(book, query).toList());
        }

        protected void findVerses(CharSequence book, int chapter, CharSequence query, List<Verse> result) {
            if (tOld != null) result.addAll(tOld.findVerses(book, chapter, query).toList());
            if (tNew != null) result.addAll(tNew.findVerses(book, chapter, query).toList());
        }

        @Override
        public String toString() {
            return getContent().get();
        }
    }

    interface OnSearchListener<I extends Item<I>> {
        void onSearchInProgress(Result<I> result, int durationInSecs);

        void onSearchCompleted(Result<I> result, long durationInMillis);
    }

}
