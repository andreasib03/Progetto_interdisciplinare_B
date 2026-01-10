package it.uninsubria.server.dao;

import java.util.ArrayList;
import java.util.List;

import it.uninsubria.shared.model.Review;

public class TestReviewDAO implements ReviewDAO {
    private int addReviewCalls = 0;
    private int addSuggestionsCalls = 0;
    private int getReviewsByBookCalls = 0;
    @Override
    public boolean addReview(int userId, int bookId, Review v) {
        addReviewCalls++;
        return true;
    }

    @Override
    public boolean updateReview(int userId, int bookId, Review review) throws Exception {
        return true;
    }

    @Override
    public List<Review> getReviewsByBook(int bookId) {
        getReviewsByBookCalls++;
        return new ArrayList<>();
    }

    public List<Review> getReviewsByBook(int userId, int bookId) {
        getReviewsByBookCalls++;
        return new ArrayList<>();
    }

    // Getters for tests
    public int getAddReviewCalls() { return addReviewCalls; }
    public int getAddSuggestionsCalls() { return addSuggestionsCalls; }
    public int getGetReviewsByBookCalls() { return getReviewsByBookCalls; }

	@Override
	public List<Review> getReviewsForBookAndUser(int userId, int bookId) throws Exception {
		return new ArrayList<>();
	}
}
