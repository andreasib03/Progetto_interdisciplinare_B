package it.uninsubria.server.dao;

import java.util.ArrayList;
import java.util.List;

import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.SuggestionEntry;

public class TestSuggestionDAO implements SuggestionDAO{

    private int getAllUserSuggestionsCalls = 0;
    private int deleteUserSuggestionCalls = 0;

    @Override
    public List<SuggestionEntry> getAllUserSuggestions(int userId) throws Exception {
        getAllUserSuggestionsCalls++;
        return new ArrayList<>();
    }

    @Override
    public boolean deleteUserSuggestion(int userId, int baseBookId, int suggestedBookId) {
        deleteUserSuggestionCalls++;
        return true;
    }

    @Override
    public boolean addSuggestion(int userId, Integer library_id, int baseBookId, int suggestedBookId) {
        return true;
    }

    @Override
    public List<Book> getUserSuggestions(int userId, int baseBookId) throws Exception {
        return new ArrayList<>();
    }

    public int getGetAllUserSuggestionsCalls() { return getAllUserSuggestionsCalls; }
    public int getDeleteUserSuggestionCalls() { return deleteUserSuggestionCalls; }

    @Override
    public List<SuggestionEntry> getSuggestionsForBaseBook(int baseBookId) throws Exception {
        return new ArrayList<>();
    }

    @Override
    public int addSuggestionReturningId(int userId, Integer libraryId, int baseBookId, int suggestedBookId)
            throws Exception {
        // Simple test implementation - return a dummy ID
        return 12345;
    }

    @Override
    public int createBatch(int userId, Integer libraryId) throws Exception {
        return 1; // dummy
    }

    @Override
    public int addToBatch(int batchId, int userId, Integer libraryId, int baseBookId, int suggestedBookId) throws Exception {
        return 1; // dummy
    }

    @Override
    public List<SuggestionEntry> getSuggestionsForBatch(int batchId) throws Exception {
        return new ArrayList<>();
    }

    @Override
    public List<Book> getUserSuggestionsForBookAndLibrary(int userId, int libraryId, int baseBookId) throws Exception {
        return new ArrayList<>(); // dummy implementation
    }

    @Override
    public boolean updateSuggestion(int userId, int suggestionId, int newSuggestedBookId) throws Exception {
        return true; // dummy implementation
    }

    @Override
    public List<SuggestionEntry> getUserSuggestionsForLibrary(int userId, int libraryId) throws Exception {
        // Simple test implementation - return empty list for testing
        return new ArrayList<>();
    }
}
