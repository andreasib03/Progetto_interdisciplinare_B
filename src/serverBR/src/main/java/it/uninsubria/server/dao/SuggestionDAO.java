package it.uninsubria.server.dao;

import java.util.List;
import it.uninsubria.shared.model.SuggestionEntry;
import it.uninsubria.shared.model.Book;

public interface SuggestionDAO {
    int addSuggestionReturningId(int userId, Integer libraryId, int baseBookId, int suggestedBookId) throws Exception;
    boolean addSuggestion(int userId, Integer libraryId, int baseBookId, int suggestedBookId) throws Exception;
    List<SuggestionEntry> getSuggestionsForBaseBook(int baseBookId) throws Exception;
    List<SuggestionEntry> getAllUserSuggestions(int userId) throws Exception;
    List<SuggestionEntry> getUserSuggestionsForLibrary(int userId, int libraryId) throws Exception;
    List<Book> getUserSuggestions(int userId, int baseBookId) throws Exception;
    List<Book> getUserSuggestionsForBookAndLibrary(int userId, int libraryId, int baseBookId) throws Exception;
    boolean deleteUserSuggestion(int userId, int baseBookId, int suggestedBookId) throws Exception;
    int createBatch(int userId, Integer libraryId) throws Exception;
    int addToBatch(int batchId, int userId, Integer libraryId, int baseBookId, int suggestedBookId) throws Exception;
    List<SuggestionEntry> getSuggestionsForBatch(int batchId) throws Exception;
    boolean updateSuggestion(int userId, int suggestionId, int newSuggestedBookId) throws Exception;
}
