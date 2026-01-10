package it.uninsubria.server.service;

import java.util.List;
import java.util.Map;

import it.uninsubria.server.cache.CacheManager;
import it.uninsubria.server.dao.ReviewDAO;
import it.uninsubria.server.util.InputValidator;
import it.uninsubria.shared.model.Review;

public class ReviewsServiceCoreImpl implements ReviewsServiceCore {
    private final ReviewDAO reviewDAO;

    public ReviewsServiceCoreImpl(ReviewDAO reviewDAO) {
        this.reviewDAO = reviewDAO;
    }

    public boolean updateReview(int userId, int bookId, Review review) throws Exception {
        return reviewDAO.updateReview(userId, bookId, review);
    }

    @Override
    public boolean addReview(int userId, int bookId, Review v) throws Exception {
        return reviewDAO.addReview(userId, bookId, v);
    }

    @Override
    public List<Review> getReviewsByBook(int bookId) throws Exception {
        return reviewDAO.getReviewsByBook(bookId);
    }

    @Override
    public boolean saveReview(int userId, int bookId, Integer libraryId, Map<String, Integer> scores, Map<String, String> notes) throws Exception {
        // Validate inputs
        InputValidator.validateId(userId, "userId");
        InputValidator.validateId(bookId, "bookId");

        int styleScore = scores.getOrDefault("Stile", 1);
        int contentScore = scores.getOrDefault("Contenuto", 1);
        int pleasantnessScore = scores.getOrDefault("Gradevolezza", 1);
        int odnessScore = scores.getOrDefault("Originalità", 1);
        int editionScore = scores.getOrDefault("Edizione", 1);

        // Validate all scores
        InputValidator.validateReviewScore(styleScore, "Style score");
        InputValidator.validateReviewScore(contentScore, "Content score");
        InputValidator.validateReviewScore(pleasantnessScore, "Pleasantness score");
        InputValidator.validateReviewScore(odnessScore, "Originality score");
        InputValidator.validateReviewScore(editionScore, "Edition score");

        // Create review with the 5 criteria scores
        String noteStyleScore = notes.getOrDefault("Stile", "");
        String noteContentScore = notes.getOrDefault("Contenuto", "");
        String notePleasantnessScore = notes.getOrDefault("Gradevolezza", "");
        String noteOdnessScore = notes.getOrDefault("Originalità", "");
        String noteEditionScore = notes.getOrDefault("Edizione", "");
        String noteFinalScore = notes.getOrDefault("Voto Finale", "");

        // Validate all notes
        InputValidator.validateReviewNote(noteStyleScore, "Style note");
        InputValidator.validateReviewNote(noteContentScore, "Content note");
        InputValidator.validateReviewNote(notePleasantnessScore, "Pleasantness note");
        InputValidator.validateReviewNote(noteOdnessScore, "Originality note");
        InputValidator.validateReviewNote(noteEditionScore, "Edition note");
        InputValidator.validateReviewNote(noteFinalScore, "Final note");

        Review review = new Review(noteStyleScore, noteContentScore, notePleasantnessScore,
                                   noteOdnessScore, noteEditionScore, noteFinalScore,
                                   styleScore, contentScore, pleasantnessScore,
                                   odnessScore, editionScore, 0); // finalScore will be calculated

        // Automatically calculate the final score as average of the 5 criteria
        int calculatedFinalScore = review.calculateFinalScore();
        review.setFinalScore(calculatedFinalScore);

        if (libraryId != null) {
            InputValidator.validateId(libraryId, "libraryId");
            review.setLibraryId(libraryId);
        }
        boolean result = reviewDAO.addReview(userId, bookId, review);

        // Invalidate related caches
        if (result) {
            CacheManager.invalidateBookData("global", bookId);
        }

        return result;
    }

    @Override
    public List<Review> getReviewsByBook(int userId, int bookId) throws Exception {
        return reviewDAO.getReviewsByBook(userId, bookId);
    }

    @Override
    public List<Review> getReviewsForBookAndUser(int userId, int bookId) throws Exception {
        return reviewDAO.getReviewsForBookAndUser(userId, bookId);
    }


}
