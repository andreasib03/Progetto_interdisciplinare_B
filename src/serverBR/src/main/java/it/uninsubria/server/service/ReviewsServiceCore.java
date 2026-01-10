package it.uninsubria.server.service;

import java.util.List;
import java.util.Map;

import it.uninsubria.shared.model.Review;

public interface ReviewsServiceCore {
    List<Review> getReviewsByBook(int userId, int bookId) throws Exception;
    boolean updateReview(int userId, int bookId, Review review) throws Exception;
    boolean addReview(int userId, int bookId, Review v) throws Exception;
    List<Review> getReviewsByBook(int bookId) throws Exception;
    List<Review> getReviewsForBookAndUser(int userId, int bookId) throws Exception;
    boolean saveReview(int userId, int bookId, Integer libraryId, Map<String, Integer> scores, Map<String, String> notes) throws Exception;
}
