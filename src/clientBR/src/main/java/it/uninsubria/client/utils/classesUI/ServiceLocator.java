package it.uninsubria.client.utils.classesUI;

import it.uninsubria.shared.rmi.BookService;
import it.uninsubria.shared.rmi.LibraryService;
import it.uninsubria.shared.rmi.ReviewsService;
import it.uninsubria.shared.rmi.UserService;
import it.uninsubria.shared.rmi.SuggestionsService;
import it.uninsubria.client.di.DIContainer;

public class ServiceLocator {
    public static it.uninsubria.shared.rmi.UserService userService;
    private static BookService bookService;
    public static LibraryService libraryService;
    public static ReviewsService valutazioniService;
    public static SuggestionsService suggestionsService;

    public static void init(UserService u, BookService b, LibraryService l, ReviewsService v) {
        // Routed via DIContainer ( Phase 2 )
        DIContainer.init(u, b, l, v);
        userService = u;
        bookService = b;
        libraryService = l;
        valutazioniService = v;
    }

    public static void initAll(UserService u, BookService b, LibraryService l, ReviewsService v, SuggestionsService s) {
        init(u, b, l, v);
        suggestionsService = s;
    }

    public static BookService getBookService() {
        return bookService;
    }

    public static it.uninsubria.shared.rmi.UserService getUserService(){
        return userService;
    }

    public static LibraryService getLibraryService(){
        return libraryService;
    }

    public static ReviewsService getReviewsService(){
        return valutazioniService;
    }

    public static SuggestionsService getSuggestionsService(){
        return suggestionsService;
    }
}
