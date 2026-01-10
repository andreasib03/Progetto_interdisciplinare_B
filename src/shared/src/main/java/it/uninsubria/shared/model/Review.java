package it.uninsubria.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Review implements Serializable {
    private User user;
    private Book books;
    private String noteStyleScore;
    private String noteContentScore;
    private String notePleasantnessScore;
    private String noteOdnessScore;
    private String noteEditionScore;
    private String noteFinalScore;

    private int styleScore;
    private int contentScore;
    private int pleasantnessScore;
    private int odnessScore;
    private int editionScore;
    private int finalScore;
    private Integer libraryId;
    private List<Review> reviews;

    public Review() {
        this.reviews = new ArrayList<>();
    }

    public Review(User user, Book books, String noteStyleScore,String noteContentScore,String notePleasantnessScore,String noteOdnessScore,String noteEditionScore,String noteFinalScore,
                   int styleScore, int contentScore, int pleasantnessScore, int odnessScore, int editionScore, int finalScore){
        this.user = user;
        this.books = books;

        this.noteStyleScore = noteStyleScore != null && noteStyleScore.length() <= 256 ? noteStyleScore : "";
        this.noteContentScore = noteContentScore != null && noteContentScore.length() <= 256 ? noteContentScore : "";
        this.notePleasantnessScore = notePleasantnessScore != null && notePleasantnessScore.length() <= 256 ? notePleasantnessScore : "";
        this.noteOdnessScore = noteOdnessScore != null && noteOdnessScore.length() <= 256 ? noteOdnessScore : "";
        this.noteEditionScore = noteEditionScore != null && noteEditionScore.length() <= 256 ? noteEditionScore : "";
        this.noteFinalScore = noteFinalScore != null && noteFinalScore.length() <= 256 ? noteFinalScore : "";

        this.styleScore = styleScore;
        this.contentScore = contentScore;
        this.pleasantnessScore = pleasantnessScore;
        this.odnessScore = odnessScore;
        this.editionScore = editionScore;
        this.finalScore = finalScore;
        this.reviews = new ArrayList<>();
    }

    public Review(String noteStyleScore,String noteContentScore,String notePleasantnessScore,String noteOdnessScore,String noteEditionScore,String noteFinalScore, 
                   int styleScore, int contentScore, int pleasantnessScore, int odnessScore, int editionScore, int finalScore){
        this.noteStyleScore = noteStyleScore != null && noteStyleScore.length() <= 256 ? noteStyleScore : "";
        this.noteContentScore = noteContentScore != null && noteContentScore.length() <= 256 ? noteContentScore : "";
        this.notePleasantnessScore = notePleasantnessScore != null && notePleasantnessScore.length() <= 256 ? notePleasantnessScore : "";
        this.noteOdnessScore = noteOdnessScore != null && noteOdnessScore.length() <= 256 ? noteOdnessScore : "";
        this.noteEditionScore = noteEditionScore != null && noteEditionScore.length() <= 256 ? noteEditionScore : "";
        this.noteFinalScore = noteFinalScore != null && noteFinalScore.length() <= 256 ? noteFinalScore : "";

        this.styleScore = styleScore;
        this.contentScore = contentScore;
        this.pleasantnessScore = pleasantnessScore;
        this.odnessScore = odnessScore;
        this.editionScore = editionScore;
        this.finalScore = finalScore;
        this.reviews = new ArrayList<>();
    }

    /**
     * Calculates the final score as the rounded average of the 5 main criteria
     * (Style, Content, Pleasantness, Originality, Edition)
     * @return the calculated final score (1-5)
     */
    public int calculateFinalScore() {
        double average = (styleScore + contentScore + pleasantnessScore + odnessScore + editionScore) / 5.0;
        return (int) Math.round(average);
    }

    public double averageScore(){
        return ((styleScore + contentScore + pleasantnessScore + odnessScore + editionScore + finalScore)/6.0);
    }

    public void addReviews(Review review, Book book) {
        reviews.add(review);
        review.getBooks().addReviewsSingularBook(review);
    }

    public double mostPopular(){
        if(reviews.isEmpty()) return 0.0;
        double sum = 0;
        for(Review r : reviews){
            sum += (r.getContentScore() + r.getEditionScore() + r.getPleasantnessScore() + r.getOdnessScore() + r.getStyleScore() + r.getFinalScore());
        }
        return sum / (reviews.size() * 6);
    }

    public Book getBooks() { return books; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Integer getLibraryId() { return libraryId; }
    public void setLibraryId(Integer lib) { this.libraryId = lib; }
    public int getStyleScore() { return styleScore; }
    public int getContentScore() { return contentScore; }
    public int getPleasantnessScore() { return pleasantnessScore; }
    public int getOdnessScore() { return odnessScore; }
    public int getEditionScore() { return editionScore; }
    public int getFinalScore() { return finalScore; }

    public String getNoteStyleScore() { return noteStyleScore; }
    public String getNoteContentScore() { return noteContentScore; }
    public String getNotePleasantnessScore() { return notePleasantnessScore; }
    public String getNoteOdnessScore() { return noteOdnessScore; }
    public String getNoteEditionScore() { return noteEditionScore; }
    public String getNoteFinalScore() { return noteFinalScore; }

    // Alias methods for compatibility
    public int getStyle() { return styleScore; }
    public String getStyleNote() { return noteStyleScore; }
    public int getContent() { return contentScore; }
    public String getContentNote() { return noteContentScore; }
    public int getPleasantness() { return pleasantnessScore; }
    public String getPleasantnessNote() { return notePleasantnessScore; }
    public int getOdness() { return odnessScore; }
    public String getOdnessNote() { return noteOdnessScore; }
    public int getEditions() { return editionScore; }
    public String getEditionsNote() { return noteEditionScore; }
    public String getFinalNoteScore() { return noteFinalScore; }

    public void setUserId(int id) { if (user == null) user = new User(); user.setUser_id(id); }
    public void setBookId(int id) { if (books == null) books = new Book(); books.setBook_id(id); }
    public void setStyle(int style) { this.styleScore = style; }
    public void setStyleNote(String note) { this.noteStyleScore = note; }
    public void setContent(int content) { this.contentScore = content; }
    public void setContentNote(String note) { this.noteContentScore = note; }
    public void setPleasantness(int pleasantness) { this.pleasantnessScore = pleasantness; }
    public void setPleasantnessNote(String note) { this.notePleasantnessScore = note; }
    public void setOdness(int odness) { this.odnessScore = odness; }
    public void setOdnessNote(String note) { this.noteOdnessScore = note; }
    public void setEditions(int editions) { this.editionScore = editions; }
    public void setEditionsNote(String note) { this.noteEditionScore = note; }
    public void setFinalNoteScore(String note) { this.noteFinalScore = note; }
    public void setFinalScore(int score) { this.finalScore = score; }

    public static Review fromResultSet(ResultSet rs) throws SQLException {
        String ns = rs.getString("note_stile"); if (ns == null) ns = rs.getString("noteStyleScore");
        String nc = rs.getString("note_contenuto"); if (nc == null) nc = rs.getString("noteContentScore");
        String nPle = rs.getString("note_gradevolezza"); if (nPle == null) nPle = rs.getString("notePleasantnessScore");
        String nOd = rs.getString("note_originalita"); if (nOd == null) nOd = rs.getString("noteOdnessScore");
        String nEd = rs.getString("note_edizione"); if (nEd == null) nEd = rs.getString("noteEditionScore");
        String nF = rs.getString("note_voto_finale"); if (nF == null) nF = rs.getString("noteFinalScore");
        int stile = rs.getInt("stile"); int contenuto = rs.getInt("contenuto"); int gradevolezza = rs.getInt("gradevolezza"); int originale = rs.getInt("originalita"); int edizione = rs.getInt("edizione"); int votoFinale = rs.getInt("voto_finale");
        Review r = new Review(ns, nc, nPle, nOd, nEd, nF, stile, contenuto, gradevolezza, originale, edizione, votoFinale);
        try { int lib = rs.getInt("libraries_id"); if (!rs.wasNull()) r.setLibraryId(lib); } catch (SQLException ignore) {}
        // user is not always retrieved in this map; caller can setUser if needed
        return r;
    }
}
