package it.uninsubria.server.rmi.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;

import it.uninsubria.server.dao.ReviewDAO;
import it.uninsubria.shared.model.Review;
import it.uninsubria.shared.rmi.ReviewsService;

import it.uninsubria.server.service.ReviewsServiceCore;
import it.uninsubria.shared.rmi.ServiceException;

public class ReviewsServiceImpl extends UnicastRemoteObject implements ReviewsService{

    private final ReviewsServiceCore core;

    public ReviewsServiceImpl(ReviewDAO reviewsDAO, ReviewsServiceCore core) throws RemoteException {
        super();
        this.core = core;
    }

    private void ensureCoreInitialized() {
        if (core == null) {
            throw new IllegalStateException("ReviewsServiceCore not initialized. Use dependency injection.");
        }
    }

    @Override
    public List<Review> getReviewsByBook(int userId, int bookId) throws RemoteException {
        ensureCoreInitialized();
        try {
            return core.getReviewsByBook(userId, bookId);
        } catch (Exception e) {
            throw new ServiceException("Errore durante il recupero delle recensioni", e);
        }
    }

    @Override
    public boolean addReview(int userId, int bookId, Review v) throws RemoteException {
        ensureCoreInitialized();
        try {
            return core.addReview(userId, bookId, v);
        } catch (Exception e) {
            throw new ServiceException("Errore durante l'aggiunta della recensione", e);
        }
    }

    @Override
    public boolean updateReview(int userId, int bookId, Review review) throws RemoteException {
        ensureCoreInitialized();
        try {
            return core.updateReview(userId, bookId, review);
        } catch (Exception e) {
            throw new ServiceException("Errore durante l'aggiornamento della recensione", e);
        }
    }

    @Override
    public List<Review> getReviewsByBook(int bookId) throws RemoteException {
        ensureCoreInitialized();
        try {
            return core.getReviewsByBook(bookId);
        } catch (Exception e) {
            throw new ServiceException("Errore durante il recupero delle recensioni", e);
        }
    }

    @Override
    public List<Review> getReviewsForBookAndUser(int userId, int bookId) throws RemoteException {
        ensureCoreInitialized();
        try {
            return core.getReviewsForBookAndUser(userId, bookId);
        } catch (Exception e) {
            throw new ServiceException("Errore durante il recupero delle recensioni per utente e libro", e);
        }
    }

    @Override
    public boolean saveReview(int userId, int bookId, Integer libraryId, Map<String, Integer> scores, Map<String, String> notes) throws RemoteException {
        ensureCoreInitialized();
        try {
            return core.saveReview(userId, bookId, libraryId, scores, notes);
        } catch (Exception e) {
            throw new ServiceException("Errore nel salvataggio della recensione", e);
        }
    }


}
