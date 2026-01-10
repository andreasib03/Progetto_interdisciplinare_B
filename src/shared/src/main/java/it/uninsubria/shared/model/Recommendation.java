package it.uninsubria.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Recommendation implements Serializable{

    private Library library;
    private Book book;
    private List<Book> suggestedBooks;
    private User user;

    public Recommendation(User user, Library library, Book book, List<Book> suggestedBooks){
        this.user = user;
        this.library = library;
        this.book = book;
        this.suggestedBooks = new ArrayList<>();
    }

    public void addSuggestedBooks(Book suggestedBookMethod) {
        if (suggestedBooks.size() < 3) {
            suggestedBooks.add(suggestedBookMethod);
        }
    }


    
}
