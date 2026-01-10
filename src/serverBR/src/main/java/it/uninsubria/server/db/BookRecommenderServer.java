package it.uninsubria.server.db;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import it.uninsubria.server.dao.BookDAO;
import it.uninsubria.shared.utils.AppConstants;
import it.uninsubria.server.dao.LibraryDAO;
import it.uninsubria.server.dao.ReviewDAO;
import it.uninsubria.server.dao.UserDAO;
import it.uninsubria.server.dao.impl.BookDAOImpl;
import it.uninsubria.server.dao.impl.LibraryDAOImpl;
import it.uninsubria.server.dao.impl.ReviewDAOImpl;
import it.uninsubria.server.dao.impl.UserDAOImpl;
import it.uninsubria.server.rmi.impl.BookServiceImpl;
import it.uninsubria.server.rmi.impl.LibraryServiceImpl;
import it.uninsubria.server.rmi.impl.ReviewsServiceImpl;
import it.uninsubria.server.rmi.impl.SuggestionServiceImpl;
import it.uninsubria.server.rmi.impl.UserServiceImpl;
import it.uninsubria.shared.rmi.UserService;
import it.uninsubria.shared.rmi.BookService;
import it.uninsubria.shared.rmi.LibraryService;
import it.uninsubria.shared.rmi.ReviewsService;
import it.uninsubria.shared.rmi.SuggestionsService;
import it.uninsubria.server.util.ConnectionPoolManager;
import it.uninsubria.server.di.ServerDIContainer;
import it.uninsubria.server.dao.SuggestionDAO;
import it.uninsubria.server.dao.impl.SuggestionDAOImpl;

/**
 * Main server class for the Book Recommender application.
 * Manages RMI services and server lifecycle.
 */
public class BookRecommenderServer{

    static {
        // Enable RMI codebase code loading if needed by clients; can be restricted to local classpath
        System.setProperty("java.rmi.server.useCodebaseOnly","false");
        // If hosting a codebase, you can also set:
        // System.setProperty("java.rmi.server.codebase", "http://host:port/");
    }

    private static final java.util.logging.Logger logger = it.uninsubria.shared.utils.LoggerUtil.getLogger(BookRecommenderServer.class);
    private static Registry registry;
    private static UserService userService;
    private static BookService bookService;
    private static LibraryService libraryService;
    private static ReviewsService reviewsService;
    private static SuggestionsService suggestionsService;

    /**
     * Starts the Book Recommender server and binds RMI services.
     *
     * @throws Exception if server startup fails
     */
    public static void startServer() throws Exception{

            // DAO using DataSource for per-call connections
            javax.sql.DataSource ds = ConnectionPoolManager.getDataSource();
            UserDAO userDAO = new UserDAOImpl(ds);
            BookDAO bookDAO = new BookDAOImpl(ds);
            LibraryDAO libraryDAO = new LibraryDAOImpl(ds);
            ReviewDAO reviewDAO = new ReviewDAOImpl(ds);

            // Initialize server DI container for core wiring
            SuggestionDAO suggestionDAO = new SuggestionDAOImpl(ds);
            ServerDIContainer.init(bookDAO, libraryDAO, userDAO, reviewDAO, suggestionDAO);

            // Implementazioni RMI
            userService = new UserServiceImpl(userDAO);
            bookService = new BookServiceImpl(bookDAO);
            libraryService = new LibraryServiceImpl(libraryDAO);
            reviewsService = new ReviewsServiceImpl(reviewDAO, ServerDIContainer.getReviewsCore());
            suggestionsService = new SuggestionServiceImpl(suggestionDAO, ServerDIContainer.getSuggestionCore());

            try{
                registry = LocateRegistry.createRegistry(AppConstants.Network.RMI_REGISTRY_PORT);
                registry.list();
            }catch(RemoteException e){
                registry = LocateRegistry.createRegistry(AppConstants.Network.RMI_REGISTRY_PORT);
            }

            // RMI Registry
            registry.rebind("UserService", userService);
            registry.rebind("BookService", bookService);
            registry.rebind("LibraryService", libraryService);
            registry.rebind("ReviewsService", reviewsService);
            registry.rebind("SuggestionsService", suggestionsService);
            
            logger.info("Server RMI avviato con successo");
            logger.info(ConnectionPoolManager.getPoolStats());

    }

    /**
     * Stops the Book Recommender server and unbinds RMI services.
     *
     * @throws Exception if server shutdown fails
     */
    public static void stopServer() throws Exception {
        if (registry != null) {
            // Unbind dei servizi (opzionale, ma pulito)
            try {
                registry.unbind("UserService");
            } catch (Exception e) {
                logger.warning("UserService non trovato durante unbind: " + e.getMessage());
            }
            try {
                registry.unbind("BookService");
            } catch (Exception e) {
                logger.warning("BookService non trovato durante unbind: " + e.getMessage());
            }
            try {
                registry.unbind("LibraryService");
            } catch (Exception e) {
                logger.warning("LibraryService non trovato durante unbind: " + e.getMessage());
            }
            try {
                registry.unbind("ReviewsService");
            } catch (Exception e) {
                logger.warning("ReviewsService non trovato durante unbind: " + e.getMessage());
            }
            try {
                registry.unbind("SuggestionsService");
            } catch (Exception e) {
                logger.warning("SuggestionsService non trovato durante unbind: " + e.getMessage());
            }

            safeUnexport(userService, "UserService");
            safeUnexport(bookService, "BookService");
            safeUnexport(libraryService, "LibraryService");
            safeUnexport(reviewsService, "ReviewsService");
            safeUnexport(suggestionsService, "SuggestionsService");

            // Unexport del registry per liberare la porta
            safeUnexport(registry, "Registry");

            logger.info("Server RMI chiuso correttamente.");
            registry = null;
        }
    }
    private static void safeUnexport(Remote obj, String name){
        if (obj != null) {
            try {
                UnicastRemoteObject.unexportObject(obj, true);
                logger.info(name + " unexported.");
            } catch (NoSuchObjectException e) {
                logger.warning(name + " era già stato unexported o mai esportato.");
            } catch (Exception e) {
                logger.severe("Errore durante l'unexport di " + name + ": " + e.getMessage());
            }
        }
    }


}
