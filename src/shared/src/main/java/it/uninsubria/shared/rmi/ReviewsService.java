package it.uninsubria.shared.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import it.uninsubria.shared.model.Review;

public interface ReviewsService extends Remote{
    List<Review> getReviewsByBook(int userId, int bookId) throws RemoteException;
    boolean updateReview(int userId, int bookId, Review review) throws RemoteException;
    boolean addReview(int userId, int bookId, Review review) throws RemoteException;

    List<Review> getReviewsByBook(int bookId) throws RemoteException;
    boolean saveReview(int userId, int bookId, Integer libraryId, Map<String, Integer> scores, Map<String, String> notes) throws RemoteException;
    List<Review> getReviewsForBookAndUser(int userId, int bookId) throws RemoteException;
}
