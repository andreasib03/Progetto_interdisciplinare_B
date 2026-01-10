package it.uninsubria.server.rmi.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.logging.Logger;

import it.uninsubria.server.dao.SuggestionDAO;
import it.uninsubria.server.service.SuggestionServiceCore;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.SuggestionEntry;
import it.uninsubria.shared.rmi.ServiceException;
import it.uninsubria.shared.rmi.SuggestionsService;

public class SuggestionServiceImpl extends UnicastRemoteObject implements SuggestionsService {

    private static final Logger logger = Logger.getLogger(SuggestionServiceImpl.class.getName());
    private final SuggestionDAO dao;
    private final SuggestionServiceCore core;

    public SuggestionServiceImpl(SuggestionDAO dao, SuggestionServiceCore core) throws RemoteException {
        super();
        this.dao = dao;
        this.core = core;
    }

    private void ensureCoreInitialized() {
        if (core == null) {
            throw new IllegalStateException("SuggestionServiceCore not initialized. Use dependency injection.");
        }
    }

    // Existing API (delegate to core)
    @Override
    public List<SuggestionEntry> getAllUserSuggestions(int userId) throws RemoteException {
        ensureCoreInitialized();
        try { return core.getAllUserSuggestions(userId); } catch (Exception e) { throw new ServiceException("Errore", e); }
    }

    @Override
    public List<SuggestionEntry> getUserSuggestionsForLibrary(int userId, int libraryId) throws RemoteException {
        ensureCoreInitialized();
        try { return core.getUserSuggestionsForLibrary(userId, libraryId); } catch (Exception e) { throw new ServiceException("Errore recupero suggerimenti libreria", e); }
    }

    @Override
    public boolean addUserSuggestion(int userId, int baseBookId, int suggestedBookId) throws RemoteException {
        ensureCoreInitialized();
        try { return core.addSuggestion(userId, null, baseBookId, suggestedBookId); } catch (Exception e) { throw new ServiceException("Errore", e); }
    }

    @Override
    public boolean deleteUserSuggestion(int userId, int baseBookId, int suggestedBookId) throws RemoteException {
        ensureCoreInitialized();
        try { return core.deleteSuggestion(userId, baseBookId, suggestedBookId); } catch (Exception e) { throw new ServiceException("Errore", e); }
    }

    @Override
    public List<Book> getUserSuggestions(int userId, int baseBookId) throws RemoteException {
        ensureCoreInitialized();
        try { return core.getUserSuggestions(userId, baseBookId); } catch (Exception e) { throw new ServiceException("Errore", e); }
    }

    @Override
    public List<Book> getUserSuggestionsForBookAndLibrary(int userId, int libraryId, int baseBookId) throws RemoteException {
        ensureCoreInitialized();
        try { return core.getUserSuggestionsForBookAndLibrary(userId, libraryId, baseBookId); } catch (Exception e) { throw new ServiceException("Errore", e); }
    }

    @Override
    public List<Book> getDynamicSuggestedBooks(int baseBookId, int libraryId, int userId, int topN) throws RemoteException {
        ensureCoreInitialized();
        try { return core.getDynamicSuggestedBooks(baseBookId, libraryId, userId, topN); } catch (Exception e) { throw new ServiceException("Errore", e); }
    }

    // New batch API (Stage 4: batch support using core)
    public int createSuggestionBatch(int userId, Integer libraryId) throws RemoteException {
        try { return dao.createBatch(userId, libraryId); } catch (Exception e) { throw new ServiceException("Errore creazione batch", e); }
    }
    public java.util.List<Integer> addSuggestionsToBatch(int batchId, int userId, Integer libraryId, int baseBookId, java.util.List<Integer> suggestedBookIds) throws RemoteException {
        java.util.List<Integer> ids = new java.util.ArrayList<>();
        java.util.List<Integer> failedIds = new java.util.ArrayList<>();

        for (Integer sid : suggestedBookIds) {
            try {
                int id = dao.addToBatch(batchId, userId, libraryId, baseBookId, sid);
                ids.add(id);
            } catch (Exception e) {
                // Log failure but continue with other suggestions
                logger.warning("Failed to add suggestion for book ID " + sid + ": " + e.getMessage());
                failedIds.add(sid);
            }
        }

        // If some suggestions failed, throw an exception with details
        if (!failedIds.isEmpty()) {
            throw new ServiceException("Alcuni suggerimenti non sono stati salvati. IDs falliti: " + failedIds, null);
        }

        return ids;
    }
    public java.util.List<SuggestionEntry> getSuggestionsForBatch(int batchId) throws RemoteException {
        try { return dao.getSuggestionsForBatch(batchId); } catch (Exception e) { throw new ServiceException("Errore fetch batch", e); }
    }
    public boolean updateSuggestion(int userId, int suggestionId, int newSuggestedBookId) throws RemoteException {
        try { return dao.updateSuggestion(userId, suggestionId, newSuggestedBookId); } catch (Exception e) { throw new ServiceException("Errore update suggestion", e); }
    }
}
