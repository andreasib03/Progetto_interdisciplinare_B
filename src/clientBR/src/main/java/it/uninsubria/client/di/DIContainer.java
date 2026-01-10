package it.uninsubria.client.di;

import it.uninsubria.shared.rmi.BookService;
import it.uninsubria.shared.rmi.LibraryService;
import it.uninsubria.shared.rmi.ReviewsService;
import it.uninsubria.shared.rmi.SuggestionsService;
import it.uninsubria.shared.rmi.UserService;

public class DIContainer {
    private static UserService userService;
    private static BookService bookService;
    private static LibraryService libraryService;
    private static ReviewsService reviewsService;
    private static SuggestionsService suggestionsService;

    public static void init(UserService u, BookService b, LibraryService l, ReviewsService v) {
        userService = u;
        bookService = b;
        libraryService = l;
        reviewsService = v;
    }

    public static void init(UserService u, BookService b, LibraryService l, ReviewsService v, SuggestionsService s) {
        init(u, b, l, v);
        suggestionsService = s;
    }

    public static UserService getUserService() { return userService; }
    public static BookService getBookService() { return bookService; }
    public static LibraryService getLibraryService() { return libraryService; }
    public static ReviewsService getReviewsService() { return reviewsService; }
    public static SuggestionsService getSuggestionsService() { return suggestionsService; }
}
