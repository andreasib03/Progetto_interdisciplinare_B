package it.uninsubria.server.dao;

import java.util.List;

import it.uninsubria.shared.model.Review;

public interface ReviewDAO {
    List<Review> getReviewsByBook(int userId, int bookId) throws Exception;
    boolean updateReview(int userId, int bookId, Review review) throws Exception;
    boolean addReview(int userId, int book_id, Review review) throws Exception;
    List<Review> getReviewsByBook(int bookId) throws Exception;
    List<Review> getReviewsForBookAndUser(int userId, int bookId) throws Exception;
}
