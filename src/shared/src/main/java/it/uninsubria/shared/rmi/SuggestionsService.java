package it.uninsubria.shared.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.SuggestionEntry;

public interface SuggestionsService extends Remote {
    boolean addUserSuggestion(int userId, int baseBookId, int suggestedBookId) throws RemoteException;
    boolean deleteUserSuggestion(int userId, int baseBookId, int suggestedBookId) throws RemoteException;
    List<SuggestionEntry> getAllUserSuggestions(int userId) throws RemoteException;
    List<SuggestionEntry> getUserSuggestionsForLibrary(int userId, int libraryId) throws RemoteException;
    List<Book> getUserSuggestions(int userId, int baseBookId) throws RemoteException;
    List<Book> getUserSuggestionsForBookAndLibrary(int userId, int libraryId, int baseBookId) throws RemoteException;
    List<Book> getDynamicSuggestedBooks(int baseBookId, int libraryId, int userId, int topN) throws RemoteException;
    int createSuggestionBatch(int userId, Integer libraryId) throws RemoteException;
    List<Integer> addSuggestionsToBatch(int batchId, int userId, Integer libraryId, int baseBookId, List<Integer> suggestedBookIds) throws RemoteException;
    List<SuggestionEntry> getSuggestionsForBatch(int batchId) throws RemoteException;
    boolean updateSuggestion(int userId, int suggestionId, int newSuggestedBookId) throws RemoteException;

}