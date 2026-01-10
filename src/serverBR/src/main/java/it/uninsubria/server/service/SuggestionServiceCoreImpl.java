package it.uninsubria.server.service;

import java.util.List;
import it.uninsubria.server.dao.SuggestionDAO;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.SuggestionEntry;

public class SuggestionServiceCoreImpl implements SuggestionServiceCore {
    
    private final SuggestionDAO dao;
    public SuggestionServiceCoreImpl(SuggestionDAO dao) { this.dao = dao; }
    @Override
    public List<SuggestionEntry> getSuggestionsForBaseBook(int baseBookId) throws Exception {
        return dao.getSuggestionsForBaseBook(baseBookId);
    }

    @Override
    public List<Book> getDynamicSuggestedBooks(int baseBookId, int libraryId, int userId, int topN) throws Exception {
        // Fallback: if dynamic logic is not implemented, return static suggestions
        return new java.util.ArrayList<>();
    }

    @Override
    public List<Book> getUserSuggestions(int userId, int baseBookId) throws Exception {
        return dao.getUserSuggestions(userId, baseBookId);
    }

    @Override
    public List<Book> getUserSuggestionsForBookAndLibrary(int userId, int libraryId, int baseBookId) throws Exception {
        return dao.getUserSuggestionsForBookAndLibrary(userId, libraryId, baseBookId);
    }

    @Override
    public boolean addSuggestion(int userId, Integer library_id, int baseBookId, int suggestedBookId) throws Exception {
        return dao.addSuggestion(userId, library_id, baseBookId, suggestedBookId);
    }

    @Override
    public boolean deleteSuggestion(int userId, int baseBookId, int suggestedBookId) throws Exception {
        return dao.deleteUserSuggestion(userId, baseBookId, suggestedBookId);
    }

    @Override
    public List<SuggestionEntry> getAllUserSuggestions(int userId) throws Exception {
        return dao.getAllUserSuggestions(userId);
    }

    @Override
    public List<SuggestionEntry> getUserSuggestionsForLibrary(int userId, int libraryId) throws Exception {
        return dao.getUserSuggestionsForLibrary(userId, libraryId);
    }

    @Override
    public int addSuggestionReturningId(int userId, Integer libraryId, int baseBookId, int suggestedBookId) throws Exception {
        return dao.addSuggestionReturningId(userId, libraryId, baseBookId, suggestedBookId);
    }
}

