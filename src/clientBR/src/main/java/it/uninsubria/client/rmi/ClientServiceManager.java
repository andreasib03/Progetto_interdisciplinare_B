package it.uninsubria.client.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.shared.rmi.BookService;
import it.uninsubria.shared.rmi.LibraryService;
import it.uninsubria.shared.rmi.ReviewsService;
import it.uninsubria.shared.rmi.UserService;

public class ClientServiceManager {
    
    private final String host;
    private final int port;

    private UserService userService;
    private BookService bookService;
    private LibraryService libraryService;
    private ReviewsService reviewsService;

    public ClientServiceManager(String host, int port) throws Exception {
        this.host = host;
        this.port = port;
        connectToServices();
    }

    private void connectToServices() throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        userService = (UserService) registry.lookup("UserService");
        bookService = (BookService) registry.lookup("BookService");
        libraryService = (LibraryService) registry.lookup("LibraryService");
        reviewsService = (ReviewsService) registry.lookup("ReviewsService");
        ServiceLocator.init(userService, bookService, libraryService, reviewsService);
    }

    public UserService getUserService() {
        return userService;
    }

    public BookService getBookService() {
        return bookService;
    }

    public LibraryService getLibraryService() {
        return libraryService;
    }

    public ReviewsService getReviewsService() {
        return reviewsService;
    }
}
