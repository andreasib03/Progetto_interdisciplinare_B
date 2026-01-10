package it.uninsubria.server.dao;

import java.util.ArrayList;
import java.util.List;

import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.ReviewManager;

public class TestBookDAO implements BookDAO {
    private final Book sampleBook = new Book(1, "Test Title", "Author", 2020);
    private int findAllCalls = 0;
    private int findByTitleCalls = 0;
    private int findByAuthorCalls = 0;
    private int findByAuthorAndYearCalls = 0;
    private int findByIDCalls = 0;
    private int getAggregatedReviewsCalls = 0;
    private int getSuggestedBooksCalls = 0;
    private int findByTitleAndYearCalls = 0;


    public int getFindAllCalls() { return findAllCalls; }
    public int getFindByTitleCalls() { return findByTitleCalls; }
    public int getFindByAuthorCalls() { return findByAuthorCalls; }
    public int getFindByAuthorAndYearCalls() { return findByAuthorAndYearCalls; }
    public int getFindByIDCalls() { return findByIDCalls; }
    public int getGetAggregatedReviewsCalls() { return getAggregatedReviewsCalls; }
    public int getGetSuggestedBooksCalls() { return getSuggestedBooksCalls; }
    public int getFindByTitleAndYearCalls() { return findByTitleAndYearCalls; }

    @Override
    public Book bookDetails(int book_id) {
        findByIDCalls++;
        return sampleBook;
    }

    @Override
    public List<Book> findAll() {
        findAllCalls++;
        List<Book> list = new ArrayList<>();
        list.add(sampleBook);
        return list;
    }

    @Override
    public List<Book> findByTitle(String title) {
        findByTitleCalls++;
        List<Book> list = new ArrayList<>();
        list.add(sampleBook);
        return list;
    }

    @Override
    public List<Book> findByAuthor(String author) {
        findByAuthorCalls++;
        List<Book> list = new ArrayList<>();
        list.add(sampleBook);
        return list;
    }

    @Override
    public List<Book> findByAuthorAndYear(String autore, int anno) {
        findByAuthorAndYearCalls++;
        List<Book> list = new ArrayList<>();
        list.add(sampleBook);
        return list;
    }

    @Override
    public Book findByID(int id) {
        findByIDCalls++;
        return sampleBook;
    }

    @Override
    public List<Book> getSuggestedBooks(int idLibro) {
        getSuggestedBooksCalls++;
        List<Book> list = new ArrayList<>();
        list.add(sampleBook);
        return list;
    }

    @Override
    public List<ReviewManager> getAggregatedReviews(int idLibro) {
        getAggregatedReviewsCalls++;
        return new ArrayList<>();
    }

    @Override
    public List<Book> findByTitleAndYear(String title, int anno) throws Exception {
        findByTitleAndYearCalls++;
        List<Book> list = new ArrayList<>();
        list.add(sampleBook);
        return list;
    }

    @Override
    public List<Book> getLibraryBooks(int libraryId) throws Exception { return new ArrayList<>(); }

    @Override
    public List<Book> findAllPaged(int offset, int limit) throws Exception {
        List<Book> list = new ArrayList<>();
        list.add(sampleBook);
        return list;
    }

    @Override
    public List<Book> findByTitlePaged(String title, int offset, int limit) throws Exception {
        List<Book> list = new ArrayList<>();
        list.add(sampleBook);
        return list;
    }

    @Override
    public List<Book> findByAuthorPaged(String author, int offset, int limit) throws Exception {
        List<Book> list = new ArrayList<>();
        list.add(sampleBook);
        return list;
    }

    @Override
    public int getTotalBooksCount() throws Exception {
        return 1;
    }
}
