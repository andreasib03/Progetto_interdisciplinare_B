package it.uninsubria.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewManager implements Serializable {

    private static final long serialVersionUID = 1L;
    private List<Review> reviews;
    private String criterio;
    private double media;
    private double avgStyle;
    private double avgContent;
    private double avgPleasantness;
    private double avgOdness;
    private double avgEditions;
    private double avgFinal;

    public ReviewManager() {
        this.reviews = new ArrayList<>();
    }

    public void setMedia(double media){
        this.media = media;
    }

    public double getMedia(){
        return media;
    }

    public void setCriterio(String criterio){
        this.criterio = criterio;
    }

    public String getCriterio(){
        return criterio;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews != null ? reviews : new ArrayList<>();
    }

    public double getAvgStyle() {
        return avgStyle;
    }

    public double getAvgContent() {
        return avgContent;
    }

    public double getAvgPleasantness() {
        return avgPleasantness;
    }

    public double getAvgOdness() {
        return avgOdness;
    }

    public double getAvgEditions() {
        return avgEditions;
    }

    public double getAvgFinal() {
        return avgFinal;
    }

    public void setAvgStyle(double avgStyle) {
        this.avgStyle = avgStyle;
    }

    public void setAvgContent(double avgContent) {
        this.avgContent = avgContent;
    }

    public void setAvgPleasantness(double avgPleasantness) {
        this.avgPleasantness = avgPleasantness;
    }

    public void setAvgOdness(double avgOdness) {
        this.avgOdness = avgOdness;
    }

    public void setAvgEditions(double avgEditions) {
        this.avgEditions = avgEditions;
    }

    public void setAvgFinal(double avgFinal) {
        this.avgFinal = avgFinal;
    }

    public void addReview(Review r) { 
        reviews.add(r);
    }

    public double mostPopularScore() { 
        return 3.3;
     }

    /**
     * Returns aggregated review statistics for a book
     * @param idLibro the book ID
     * @return map containing aggregated data
     */
    public Map<String, Object> getAggregatedReviews(int idLibro) {
        Map<String, Object> result = new HashMap<>();

        if (reviews == null || reviews.isEmpty()) {
            result.put("hasReviews", false);
            result.put("totalReviews", 0);
            return result;
        }

        result.put("hasReviews", true);
        result.put("totalReviews", reviews.size());

        // Calculate averages for each criterion
        Map<String, Double> averages = new HashMap<>();
        averages.put("style", reviews.stream().mapToInt(Review::getStyle).average().orElse(0.0));
        averages.put("content", reviews.stream().mapToInt(Review::getContent).average().orElse(0.0));
        averages.put("pleasantness", reviews.stream().mapToInt(Review::getPleasantness).average().orElse(0.0));
        averages.put("originality", reviews.stream().mapToInt(Review::getOdness).average().orElse(0.0));
        averages.put("edition", reviews.stream().mapToInt(Review::getEditions).average().orElse(0.0));
        averages.put("final", reviews.stream().mapToInt(Review::getFinalScore).average().orElse(0.0));

        result.put("averages", averages);

        // Count reviews per score for each criterion (1-5 stars)
        Map<String, Map<Integer, Integer>> scoreCounts = new HashMap<>();
        for (String criterion : Arrays.asList("style", "content", "pleasantness", "originality", "edition", "final")) {
            Map<Integer, Integer> counts = new HashMap<>();
            for (int i = 1; i <= 5; i++) counts.put(i, 0);
            scoreCounts.put(criterion, counts);
        }

        // Populate counts
        for (Review review : reviews) {
            scoreCounts.get("style").put(review.getStyle(), scoreCounts.get("style").get(review.getStyle()) + 1);
            scoreCounts.get("content").put(review.getContent(), scoreCounts.get("content").get(review.getContent()) + 1);
            scoreCounts.get("pleasantness").put(review.getPleasantness(), scoreCounts.get("pleasantness").get(review.getPleasantness()) + 1);
            scoreCounts.get("originality").put(review.getOdness(), scoreCounts.get("originality").get(review.getOdness()) + 1);
            scoreCounts.get("edition").put(review.getEditions(), scoreCounts.get("edition").get(review.getEditions()) + 1);
            scoreCounts.get("final").put(review.getFinalScore(), scoreCounts.get("final").get(review.getFinalScore()) + 1);
        }

        result.put("scoreCounts", scoreCounts);

        // Collect comments (non-empty notes)
        List<Map<String, Object>> comments = new ArrayList<>();
        for (Review review : reviews) {
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("userId", review.getUser() != null ? review.getUser().getUser_id() : "Anonymous");
            commentData.put("styleNote", review.getNoteStyleScore());
            commentData.put("contentNote", review.getNoteContentScore());
            commentData.put("pleasantnessNote", review.getNotePleasantnessScore());
            commentData.put("originalityNote", review.getNoteOdnessScore());
            commentData.put("editionNote", review.getNoteEditionScore());
            commentData.put("finalNote", review.getNoteFinalScore());
            comments.add(commentData);
        }

        result.put("comments", comments);

        return result;
    }
}
