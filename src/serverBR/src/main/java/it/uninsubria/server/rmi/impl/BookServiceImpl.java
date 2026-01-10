package it.uninsubria.server.rmi.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import it.uninsubria.shared.rmi.ServiceException;

import it.uninsubria.server.dao.BookDAO;
import it.uninsubria.shared.model.Book;
import it.uninsubria.shared.model.ReviewManager;
import it.uninsubria.shared.rmi.BookService;
import it.uninsubria.shared.model.SuggestionEntry;
import it.uninsubria.server.di.ServerDIContainer;
import it.uninsubria.server.service.BookServiceCore;
import it.uninsubria.server.ServerMessageManager;
import it.uninsubria.server.service.BookServiceCoreImpl;

/**
 * RMI implementation of BookService, delegating to BookServiceCore for business logic.
 */
public class BookServiceImpl extends UnicastRemoteObject implements BookService {

    private final BookDAO bookDAO;
    private final BookServiceCore bookCore;

    /**
     * Constructs a BookServiceImpl with the given BookDAO.
     *
     * @param bookDAO the BookDAO instance
     * @throws RemoteException if RMI setup fails
     */
    public BookServiceImpl(BookDAO bookDAO) throws RemoteException {
        super();
        this.bookDAO = bookDAO;
        // If a specific DAO is provided, create a dedicated core for it (for testing)
        // Otherwise use the global ServerDIContainer core
        if (bookDAO != null) {
            this.bookCore = new BookServiceCoreImpl(bookDAO);
        } else {
            BookServiceCore core = ServerDIContainer.getBookCore();
            if (core != null) {
                this.bookCore = core;
            } else {
                throw new IllegalStateException("No BookDAO provided and no global core available");
            }
        }
    }

    @Override
    public List<Book> searchGlobally() throws RemoteException {
        try {
            return bookCore.searchGlobally();
        } catch (Exception e) {
            throw new ServiceException(ServerMessageManager.getString("server.error.search.title"), e);
        }
    }

    @Override
    public List<Book> searchByTitle(String title) throws RemoteException {
        try{
            return bookCore.searchByTitle(title);
        }catch(Exception e){
            throw new ServiceException(ServerMessageManager.getString("server.error.search.title"), e);
        }
    }

    @Override
    public List<Book> searchByAuthor(String author) throws RemoteException {
        try{
            return bookCore.searchByAuthor(author);
        }catch(Exception e){
            throw new ServiceException(ServerMessageManager.getString("server.error.search.author"), e);
        }
    }

    @Override
    public List<Book> searchByAuthorAndYear(String author, int year) throws RemoteException {
        try{
            return bookCore.searchByAuthorAndYear(author, year);
        }catch(Exception e){
            throw new ServiceException("Errore durante la ricerca per autore e anno", e);
        }
    }

    @Override
    public List<Book> searchByTitleAndYear(String title, int year) throws RemoteException {
        try{
            return bookCore.searchByTitleAndYear(title, year);
        }catch(Exception e){
            throw new ServiceException("Errore durante la ricerca per titolo e anno", e);
        }
    }

    @Override
    public Book getBookDetails(int bookId) throws RemoteException {
        try{
            return bookCore.getBookDetails(bookId);
        }catch(Exception e){
            throw new ServiceException("Errore durante il recupero dei dettagli del libro", e);
        }
    }

    @Override
    public List<ReviewManager> getAggregatedReviews(int bookId) throws RemoteException {
        try {
            return bookCore.getAggregatedReviews(bookId);
        } catch (Exception e) {
            throw new ServiceException("Errore durante il recupero delle valutazioni del libro", e);
        }
    }

    @Override
    public List<Book> getLibraryBooks(int libraryId) throws RemoteException {
        try {
            return bookCore.getLibraryBooks(libraryId);
        } catch (Exception e) {
            throw new ServiceException("Errore durante il recupero dei libri della libreria", e);
        }
    }

    @Override
    public List<SuggestionEntry> getAllUserSuggestions(int userId) throws RemoteException {
        try {
            var suggestionsService = ServerDIContainer.getSuggestionsService();
            if (suggestionsService != null) {
                return suggestionsService.getAllUserSuggestions(userId);
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            throw new ServiceException("Errore nel recupero suggerimenti utente", e);
        }
    }
    


    @Override
    public List<SuggestionEntry> getUserSuggestionsForLibrary(int userId, int libraryId) throws RemoteException {
        try {
            var suggestionsService = ServerDIContainer.getSuggestionsService();
            if (suggestionsService != null) {
                return suggestionsService.getUserSuggestionsForLibrary(userId, libraryId);
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            throw new ServiceException("Errore nel recupero suggerimenti libreria", e);
        }
    }

    @Override
    public List<Book> getUserSuggestionsForBookAndLibrary(int userId, int libraryId, int baseBookId) throws RemoteException {
        try {
            var core = ServerDIContainer.getSuggestionCore();
            if (core != null) {
                return core.getUserSuggestionsForBookAndLibrary(userId, libraryId, baseBookId);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            throw new ServiceException("Errore recupero suggerimenti per libro e libreria", e);
        }
    }

    @Override
    public boolean addUserSuggestion(int userId, int baseBookId, Integer libraryId, int suggestedBookId) throws RemoteException {
        try {
            var core = ServerDIContainer.getSuggestionCore();
            if (core != null) {
                return core.addSuggestion(userId, libraryId, baseBookId, suggestedBookId);
            }
        } catch (Exception e) {
            throw new ServiceException("Errore durante l'aggiunta di un suggerimento (4-arg)", e);
        }
		return false;
    }

    @Override
    public int addUserSuggestionReturningId(int userId, int baseBookId, Integer libraryId, int suggestedBookId) throws RemoteException {
        try {
            var core = ServerDIContainer.getSuggestionCore();
            if (core != null) {
                return core.addSuggestionReturningId(userId, libraryId, baseBookId, suggestedBookId);
            }
            boolean ok = addUserSuggestion(userId, baseBookId, libraryId, suggestedBookId);
            return ok ? 1 : -1;
        } catch (Exception e) {
            throw new ServiceException("Errore durante l'aggiunta di un suggerimento ReturningId", e);
        }
    }

    // Patch B: deleteUserSuggestion (3-arg) – create per compatibilità UI
    @Override
    public boolean deleteUserSuggestion(int userId, int baseBookId, int suggestedBookId) throws RemoteException {
        try {
            var dao = it.uninsubria.server.di.ServerDIContainer.getSuggestionDAO();
            if (dao != null) {
                return dao.deleteUserSuggestion(userId, baseBookId, suggestedBookId);
            }
            return true; // fallback permissivo
        } catch (Exception e) {
            throw new ServiceException("Errore durante l'eliminazione del suggerimento", e);
        }
    }

    @Override
    public void invalidateBooksCache() throws RemoteException {
        try { bookCore.invalidateBooksCache(); } catch (Exception e){ throw new ServiceException("Errore invalidando cache dei libri", e); }
    }

    @Override
    public int createSuggestionBatch(int userId, Integer libraryId) throws RemoteException {
        try {
            var suggestionsService = ServerDIContainer.getSuggestionsService();
            if (suggestionsService != null) {
                return suggestionsService.createSuggestionBatch(userId, libraryId);
            }
            throw new ServiceException("SuggestionsService not available");
        } catch (Exception e) {
            throw new ServiceException("Errore nella creazione del batch di suggerimenti", e);
        }
    }

    @Override
    public List<Integer> addSuggestionsToBatch(int batchId, int userId, Integer libraryId, int baseBookId, List<Integer> suggestedBookIds) throws RemoteException {
        try {
            var suggestionsService = ServerDIContainer.getSuggestionsService();
            if (suggestionsService != null) {
                return suggestionsService.addSuggestionsToBatch(batchId, userId, libraryId, baseBookId, suggestedBookIds);
            }
            throw new ServiceException("SuggestionsService not available");
        } catch (Exception e) {
            throw new ServiceException("Errore nell'aggiunta dei suggerimenti al batch", e);
        }
    }

    @Override
    public List<Book> searchGloballyPaged(int offset, int limit) throws RemoteException {
        try {
            return bookCore.searchGloballyPaged(offset, limit);
        } catch (Exception e) {
            throw new ServiceException("Errore durante la ricerca globale paginata", e);
        }
    }

    @Override
    public List<Book> searchByTitlePaged(String query, int offset, int limit) throws RemoteException {
        try {
            return bookCore.searchByTitlePaged(query, offset, limit);
        } catch (Exception e) {
            throw new ServiceException("Errore durante la ricerca per titolo paginata", e);
        }
    }

    @Override
    public List<Book> searchByAuthorPaged(String author, int offset, int limit) throws RemoteException {
        try {
            return bookCore.searchByAuthorPaged(author, offset, limit);
        } catch (Exception e) {
            throw new ServiceException("Errore durante la ricerca per autore paginata", e);
        }
    }

    @Override
    public int getTotalBooksCount() throws RemoteException {
        try {
            return bookCore.getTotalBooksCount();
        } catch (Exception e) {
            throw new ServiceException("Errore durante il conteggio totale dei libri", e);
        }
    }

    // ===== NUOVI METODI PER CACHE PER-USER =====

    @Override
    public List<Book> searchGloballyForUser(String userId) throws RemoteException {
        try {
            return bookCore.searchGloballyForUser(userId);
        } catch (Exception e) {
            throw new ServiceException("Errore durante la ricerca globale per utente", e);
        }
    }

    @Override
    public List<Book> searchByTitleForUser(String userId, String title) throws RemoteException {
        try {
            return bookCore.searchByTitleForUser(userId, title);
        } catch (Exception e) {
            throw new ServiceException(ServerMessageManager.getString("server.error.search.title.year"), e);
        }
    }

    @Override
    public List<Book> searchByAuthorForUser(String userId, String author) throws RemoteException {
        try {
            return bookCore.searchByAuthorForUser(userId, author);
        } catch (Exception e) {
            throw new ServiceException(ServerMessageManager.getString("server.error.search.author.year"), e);
        }
    }

    @Override
    public List<Book> searchByAuthorAndYearForUser(String userId, String author, int year) throws RemoteException {
        try {
            return bookCore.searchByAuthorAndYearForUser(userId, author, year);
        } catch (Exception e) {
            throw new ServiceException("Errore durante la ricerca per autore e anno per utente", e);
        }
    }

    @Override
    public List<Book> searchByTitleAndYearForUser(String userId, String title, int year) throws RemoteException {
        try {
            return bookCore.searchByTitleAndYearForUser(userId, title, year);
        } catch (Exception e) {
            throw new ServiceException("Errore durante la ricerca per titolo e anno per utente", e);
        }
    }

    @Override
    public Book getBookDetailsForUser(String userId, int bookId) throws RemoteException {
        try {
            return bookCore.getBookDetailsForUser(userId, bookId);
        } catch (Exception e) {
            throw new ServiceException("Errore durante il recupero dei dettagli del libro per utente", e);
        }
    }

    @Override
    public List<ReviewManager> getAggregatedReviewsForUser(String userId, int bookId) throws RemoteException {
        try {
            return bookCore.getAggregatedReviewsForUser(userId, bookId);
        } catch (Exception e) {
            throw new ServiceException("Errore durante il recupero delle valutazioni per utente", e);
        }
    }

    @Override
    public List<Book> getLibraryBooksForUser(String userId, int libraryId) throws RemoteException {
        try {
            return bookCore.getLibraryBooksForUser(userId, libraryId);
        } catch (Exception e) {
            throw new ServiceException("Errore durante il recupero dei libri della libreria per utente", e);
        }
    }

    @Override
    public void invalidateBooksCacheForUser(String userId) throws RemoteException {
        try {
            bookCore.invalidateBooksCacheForUser(userId);
        } catch (Exception e) {
            throw new ServiceException("Errore durante l'invalidazione della cache libri per utente", e);
        }
    }

    @Override
    public List<Book> searchGloballyPagedForUser(String userId, int offset, int limit) throws RemoteException {
        try {
            return bookCore.searchGloballyPagedForUser(userId, offset, limit);
        } catch (Exception e) {
            throw new ServiceException("Errore durante la ricerca globale paginata per utente", e);
        }
    }

    @Override
    public List<Book> searchByTitlePagedForUser(String userId, String query, int offset, int limit) throws RemoteException {
        try {
            return bookCore.searchByTitlePagedForUser(userId, query, offset, limit);
        } catch (Exception e) {
            throw new ServiceException("Errore durante la ricerca per titolo paginata per utente", e);
        }
    }

    @Override
    public List<Book> searchByAuthorPagedForUser(String userId, String author, int offset, int limit) throws RemoteException {
        try {
            return bookCore.searchByAuthorPagedForUser(userId, author, offset, limit);
        } catch (Exception e) {
            throw new ServiceException("Errore durante la ricerca per autore paginata per utente", e);
        }
    }

    @Override
    public List<Book> getSuggestedBooks(int bookId) throws RemoteException {
        try {
            return bookDAO.getSuggestedBooks(bookId);
        } catch (Exception e) {
            throw new ServiceException("Errore nel recupero dei libri suggeriti", e);
        }
    }
}
