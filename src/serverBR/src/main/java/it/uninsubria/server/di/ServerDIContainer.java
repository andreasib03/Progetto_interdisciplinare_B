package it.uninsubria.server.di;


import it.uninsubria.server.dao.BookDAO;
import it.uninsubria.server.dao.LibraryDAO;
import it.uninsubria.server.dao.ReviewDAO;
import it.uninsubria.server.dao.UserDAO;
import it.uninsubria.server.dao.SuggestionDAO;
import it.uninsubria.server.service.BookServiceCore;
import it.uninsubria.server.service.BookServiceCoreImpl;
import it.uninsubria.server.service.LibraryServiceCore;
import it.uninsubria.server.service.LibraryServiceCoreImpl;
import it.uninsubria.server.service.UserServiceCore;
import it.uninsubria.server.service.UserServiceCoreImpl;
import it.uninsubria.server.service.ReviewsServiceCore;
import it.uninsubria.server.service.ReviewsServiceCoreImpl;
import it.uninsubria.server.service.SuggestionServiceCore;
import it.uninsubria.server.service.SuggestionServiceCoreImpl;
import it.uninsubria.shared.rmi.SuggestionsService;

public class ServerDIContainer {
    private static BookDAO bookDAO;
    private static LibraryDAO libraryDAO;
    private static UserDAO userDAO;
    private static ReviewDAO reviewDAO;
    private static SuggestionDAO suggestionDAO;
    private static BookServiceCore bookCore;
    private static LibraryServiceCore libraryCore;
    private static UserServiceCore userCore;
    private static ReviewsServiceCore reviewsCore;
    private static SuggestionServiceCore suggestionCore;
    private static SuggestionsService suggestionsService;

    public static void init(BookDAO b, LibraryDAO l, UserDAO u, ReviewDAO r) {
        bookDAO = b;
        libraryDAO = l;
        userDAO = u;
        reviewDAO = r;
        bookCore = new BookServiceCoreImpl(b);
        libraryCore = new LibraryServiceCoreImpl(l);
        userCore = new UserServiceCoreImpl(u);
        reviewsCore = new ReviewsServiceCoreImpl(r);
    }

    // Overload to include SuggestionDAO wiring
    public static void init(BookDAO b, LibraryDAO l, UserDAO u, ReviewDAO r, SuggestionDAO s) {
        bookDAO = b;
        libraryDAO = l;
        userDAO = u;
        reviewDAO = r;
        suggestionDAO = s;
        bookCore = new BookServiceCoreImpl(b);
        libraryCore = new LibraryServiceCoreImpl(l);
        userCore = new UserServiceCoreImpl(u);
        reviewsCore = new ReviewsServiceCoreImpl(r);
        suggestionCore = new SuggestionServiceCoreImpl(s);
    }

    public static BookServiceCore getBookCore() { return bookCore; }
    public static LibraryServiceCore getLibraryCore() { return libraryCore; }
    public static UserServiceCore getUserCore() { return userCore; }
    public static ReviewsServiceCore getReviewsCore() { return reviewsCore; }
    public static SuggestionServiceCore getSuggestionCore() { return suggestionCore; }
    public static SuggestionDAO getSuggestionDAO() { return suggestionDAO; }
    public static SuggestionsService getSuggestionsService() { return suggestionsService; }
    public static void setSuggestionsService(SuggestionsService service) { suggestionsService = service; }

    // Optional: expose DAOs if needed for fallbacks
    public static BookDAO getBookDAO() { return bookDAO; }
    public static LibraryDAO getLibraryDAO() { return libraryDAO; }
    public static UserDAO getUserDAO() { return userDAO; }
    public static ReviewDAO getReviewDAO() { return reviewDAO; }
}
