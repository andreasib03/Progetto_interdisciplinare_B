package it.uninsubria.server.service;

import java.util.List;
import it.uninsubria.shared.model.*;

public interface SuggestionServiceCore {
    List<SuggestionEntry> getSuggestionsForBaseBook(int baseBookId) throws Exception;
    boolean addSuggestion(int userId, Integer libraryId, int baseBookId, int suggestedBookId) throws Exception;
    int addSuggestionReturningId(int userId, Integer libraryId, int baseBookId, int suggestedBookId) throws Exception;
    boolean deleteSuggestion(int userId, int baseBookId, int suggestedBookId) throws Exception;
    List<SuggestionEntry> getAllUserSuggestions(int userId) throws Exception;
    List<SuggestionEntry> getUserSuggestionsForLibrary(int userId, int libraryId) throws Exception;
    List<Book> getUserSuggestions(int userId, int baseBookId) throws Exception;
    List<Book> getUserSuggestionsForBookAndLibrary(int userId, int libraryId, int baseBookId) throws Exception;
    List<Book> getDynamicSuggestedBooks(int baseBookId, int libraryId, int userId, int topN) throws Exception;
}
