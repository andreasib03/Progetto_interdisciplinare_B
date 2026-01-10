package it.uninsubria.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
/**
 * Represents a book entity with details like title, authors, description, etc.
 */
public class Book implements Serializable {
    private int book_id;
    private String title;
    private String authors;
    private String description;
    private String category;
    private String publisher;
    private String price;
    private String publisher_month;
    private int publish_date_year;
    /** Property for UI selection state. */
    private transient BooleanProperty selected = new SimpleBooleanProperty(false);

    private List<Review> reviews;

    /**
     * Default constructor.
     */
    public Book() {
    }

    @Deprecated(since = "1.0", forRemoval = true)
    /**
     * Constructs a Book without ID.
     *
     * @param title the book title
     * @param authors the book authors
     * @param publish_date_year the publication year
     */
    public Book(String title, String authors, int publish_date_year) {
        this.title = title;
        this.authors = authors;
        this.publish_date_year = publish_date_year;
    }

    @Deprecated(since = "1.0", forRemoval = true)
    /**
     * Constructs a Book with basic information.
     *
     * @param id the book ID
     * @param title the book title
     * @param authors the book authors
     * @param publish_date_year the publication year
     */
    public Book(int id, String title, String authors, int publish_date_year) {
        this.book_id = id;
        this.title = title;
        this.authors = authors;
        this.publish_date_year = publish_date_year;
    }

    @Deprecated(since = "1.0", forRemoval = true)
    /**
     * Constructs a Book with detailed information.
     *
     * @param title the book title
     * @param authors the book authors
     * @param publish_date_year the publication year
     * @param price the book price
     * @param description the book description
     * @param publisher the publisher
     * @param category the category
     */
    public Book(String title, String authors, int publish_date_year, String price, String description, String publisher, String category){
        this.title = title;
        this.authors = authors;
        this.publish_date_year = publish_date_year;
        this.price = price;
        this.description = description;
        this.publisher = publisher;
        this.category = category;
    }

    @Deprecated(since = "1.0", forRemoval = true)
     /**
     * Constructs a Book with ID and full details.
     *
     * @param id the book ID
     * @param title the book title
     * @param authors the book authors
     * @param description the book description
     * @param category the category
     * @param publisher the publisher
     * @param price the price
     * @param publisher_month the publication month
     * @param publish_date_year the publication year
     */
    public Book(int id, String title, String authors, String description, String category, String publisher, String price, String publisher_month, int publish_date_year ){
        this.book_id = id;
        this.title = title;
        this.authors = authors;
        this.publish_date_year = publish_date_year;
        this.price = price;
        this.description = description;
        this.publisher = publisher;
        this.publisher_month = publisher_month;
        this.category = category;
    }
    public boolean isSelected() { 
        return selectedProperty().get();
    }
    public void setSelected(boolean selected) {
        selectedProperty().set(selected);
    }

    /**
     * Gets the selected property for JavaFX binding.
     *
     * @return the selected property
     */
    public BooleanProperty selectedProperty() {
        return selected;
    }

    /**
     * Adds a review to this book.
     *
     * @param review the review to add
     */
    public void addReviewsSingularBook(Review review){
        if (reviews == null) {
            reviews = new ArrayList<>();
        }
        reviews.add(review);
    }

    @Override
    public String toString() {
        return title + " - " + authors + " (" + publish_date_year + ")";
    }

    /**
     * Creates a Book instance from a ResultSet.
     *
     * @param rs the ResultSet
     * @return the Book instance
     * @throws java.sql.SQLException if database access fails
     */
    public static Book fromResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Book(
            rs.getInt("book_id"),
            rs.getString("title"),
            rs.getString("authors"),
            rs.getString("descriptions"), // Nota: il campo nel DB è "descriptions" ma nella classe è "description"
            rs.getString("category"),
            rs.getString("publisher"),
            rs.getString("price"),
            rs.getString("publish_date_month"),
            rs.getInt("publish_date_year")
        );
    }
}
