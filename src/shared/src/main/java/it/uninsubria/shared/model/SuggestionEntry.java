package it.uninsubria.shared.model;

import java.io.Serializable;

public class SuggestionEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private int suggested_id;
    private int userId;
    private int baseBookId;
    private int suggestedBookId;
    private String baseBookTitle;
    private String suggestedBookTitle;
    private int library_id;

    public SuggestionEntry() {}

    public SuggestionEntry(int userId, int baseBookId, int suggestedBookId, String baseBookTitle, String suggestedBookTitle) {
        this.userId = userId;
        this.baseBookId = baseBookId;
        this.suggestedBookId = suggestedBookId;
        this.baseBookTitle = baseBookTitle;
        this.suggestedBookTitle = suggestedBookTitle;
    }

    public SuggestionEntry(int suggested_id, int userId, int library_id, int baseBookId, int suggestedBookId, java.sql.Timestamp createdAt) {
        this.userId = userId;
        this.baseBookId = baseBookId;
        this.suggestedBookId = suggestedBookId;
        this.suggested_id = suggested_id;
        this.library_id = library_id;

    }

    public int getUserId() { return userId; }
    public void setUserId(int v) { userId = v; }
    public int getBaseBookId() { return baseBookId; }
    public void setBaseBookId(int v) { baseBookId = v; }
    public int getSuggestedBookId() { return suggestedBookId; }
    public void setSuggestedBookId(int v) { suggestedBookId = v; }
    public String getBaseBookTitle() { return baseBookTitle; }
    public void setBaseBookTitle(String v) { baseBookTitle = v; }
    public String getSuggestedBookTitle() { return suggestedBookTitle; }
    public void setSuggestedBookTitle(String v) { suggestedBookTitle = v; }

    public int getSuggestedId() { return suggested_id; }
    public int getLibraryId() { return library_id; }
}
