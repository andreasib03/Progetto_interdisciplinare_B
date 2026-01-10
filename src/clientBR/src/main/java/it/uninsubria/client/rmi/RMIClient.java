package it.uninsubria.client.rmi;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

import it.uninsubria.shared.rmi.BookService;
import it.uninsubria.shared.rmi.LibraryService;
import it.uninsubria.shared.rmi.ReviewsService;
import it.uninsubria.shared.rmi.UserService;
import it.uninsubria.shared.utils.AppConfig;
import it.uninsubria.shared.utils.AppConstants;


public class RMIClient {

    private static final Logger logger = Logger.getLogger(RMIClient.class.getName());

    private static final int PORT = AppConstants.Network.RMI_REGISTRY_PORT;

    private static UserService userService;
    private static BookService bookService;
    private static LibraryService libraryService;
    private static ReviewsService reviewsService;


    public static BookService getBookService() {
        if (bookService == null) {
            bookService = (BookService) lookupService("BookService");
        }
        return bookService;
    }

    public static LibraryService getLibraryService() {
        if (libraryService == null) {
            libraryService = (LibraryService) lookupService("LibraryService");
        }
        return libraryService;
    }

    public static ReviewsService getValutazioniService() {
        if (reviewsService == null) {
            reviewsService = (ReviewsService) lookupService("ReviewsService");
        }
        return reviewsService;
    }

    private static Object lookupService(String serviceName) {
        try {
            String url = "rmi://" + AppConfig.getServerHost() + ":" + PORT + "/" + serviceName;
            return Naming.lookup(url);
        } catch (Exception e) {
            logger.severe("Errore durante il lookup del servizio RMI: " + serviceName + " - " + e.getMessage());
            return null;
        }
    }


    public static UserService getUserService() {
        if (userService == null) {
            userService = (UserService) lookupService("UserService");
        }
        return userService;
    }

    
    public static void connectToServer(String host) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, AppConstants.Network.RMI_REGISTRY_PORT);
        userService = (UserService) registry.lookup("UserService");
    }

}
