package it.uninsubria.server.rmi.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uninsubria.server.dao.LibraryDAO;
import it.uninsubria.shared.model.Library;
import it.uninsubria.shared.rmi.LibraryService;

import it.uninsubria.server.service.LibraryServiceCore;
import it.uninsubria.server.di.ServerDIContainer;
import it.uninsubria.shared.rmi.ServiceException;

public class LibraryServiceImpl extends UnicastRemoteObject implements LibraryService {
    private static final Logger logger = LoggerFactory.getLogger(LibraryServiceImpl.class);

    private final LibraryDAO libraryDAO;

    public LibraryServiceImpl(LibraryDAO libraryDAO) throws RemoteException {
        super();
        this.libraryDAO = libraryDAO;
    }

    @Override
    public List<Library> getUserLibraries(String username) throws RemoteException {
        try {
            LibraryServiceCore core = ServerDIContainer.getLibraryCore();
            if (core != null) return core.getUserLibraries(username);
            return libraryDAO.getUserLibraries(username);
        } catch (SQLException e) {
            logger.error("SQL error nel recupero delle librerie per utente {}: {}", username, e.getMessage());
            throw new ServiceException("Errore di accesso al database durante il recupero delle librerie.", e);
        } catch (Exception e) {
            logger.error("Errore generico nel recupero delle librerie per utente {}: {}", username, e.getMessage());
            throw new ServiceException("Errore interno del server durante il recupero delle librerie.", e);
        }
    }

    @Override
    public List<Integer> getBooksInLibrariesImpl(int idLibreria) throws RemoteException {
        try {
            LibraryServiceCore core = ServerDIContainer.getLibraryCore();
            if (core != null) return core.getBooksInLibraries(idLibreria);
            return libraryDAO.getBooksInLibraries(idLibreria);
        } catch (SQLException e) {
            logger.error("SQL error nel recupero dei libri per la libreria {}: {}", idLibreria, e.getMessage());
            throw new ServiceException("Errore di accesso al database durante il recupero dei libri.", e);
        } catch (Exception e) {
            logger.error("Errore generico nel recupero dei libri per la libreria {}: {}", idLibreria, e.getMessage());
            throw new ServiceException("Errore interno del server durante il recupero dei libri.", e);
        }
    }

    @Override
    public boolean libraryExistsForUser(String username, String libraryName) throws RemoteException {
        try {
            LibraryServiceCore core = ServerDIContainer.getLibraryCore();
            if (core != null) return core.libraryExistsForUser(username, libraryName);
            return libraryDAO.libraryExistsForUser(username, libraryName);
        } catch (SQLException e) {
            logger.error("SQL error nel verificare l'esistenza della libreria '{}' per utente {}: {}", libraryName, username, e.getMessage());
            throw new ServiceException("Errore di accesso al database durante la verifica dell'esistenza della libreria.", e);
        } catch (Exception e) {
            logger.error("Errore generico nel verificare la libreria per l'utente {}: {}", username, e.getMessage());
            throw new ServiceException("Errore interno del server durante la verifica della libreria.", e);
        }
    }

    @Override
    public Library addLibraryForUser(String username, Library library) throws RemoteException {
        try {
            LibraryServiceCore core = ServerDIContainer.getLibraryCore();
            if (core != null) {
                // Se il core è presente, delega a lui
                return core.addLibraryForUser(username, library);
            }
            return libraryDAO.addLibraryForUser(username, library);
        } catch (SQLException e) {
            logger.error("SQL error nell'aggiungere la libreria '{}' per utente {}: {}", library.getName(), username, e.getMessage());
            throw new ServiceException("Errore di accesso al database durante l'inserimento della libreria.", e);
        } catch (Exception e) {
            logger.error("Errore generico nell'aggiungere la libreria '{}' per utente {}: {}", library.getName(), username, e.getMessage());
            throw new ServiceException("Errore interno del server durante l'inserimento della libreria.", e);
        }
    }

    @Override
    public void updateLibraryForUser(String username, Library oldLibrary, Library newLibrary) throws RemoteException {
        try {
            LibraryServiceCore core = ServerDIContainer.getLibraryCore();
            if (core != null) {
                core.updateLibraryForUser(username, oldLibrary, newLibrary);
                return;
            }
            libraryDAO.updateLibraryForUser(username, oldLibrary, newLibrary);
        } catch (SQLException e) {
            logger.error("SQL error durante l'aggiornamento della libreria per utente {}: {}", username, e.getMessage());
            throw new ServiceException("Errore di accesso al database durante l'aggiornamento della libreria.", e);
        } catch (Exception e) {
            logger.error("Errore generico durante l'aggiornamento della libreria per utente {}: {}", username, e.getMessage());
            throw new ServiceException("Errore interno del server durante l'aggiornamento della libreria.", e);
        }
    }

    @Override
    public void deleteLibraryForUser(String username, Library library) throws RemoteException {
        System.out.println("DEBUG LibraryServiceImpl: RMI call to delete library '" + library.getName() + "' (ID: " + library.getId() + ") for user '" + username + "'");
        try {
            LibraryServiceCore core = ServerDIContainer.getLibraryCore();
            if (core != null) {
                core.deleteLibraryForUser(username, library);
                return;
            }
            libraryDAO.deleteLibraryForUser(username, library);
        } catch (SQLException e) {
            logger.error("SQL error durante l'eliminazione della libreria per utente {}: {}", username, e.getMessage());
            throw new ServiceException("Errore di accesso al database durante l'eliminazione della libreria.", e);
        } catch (Exception e) {
            logger.error("Errore generico durante l'eliminazione della libreria per utente {}: {}", username, e.getMessage());
            throw new ServiceException("Errore interno del server durante l'eliminazione della libreria.", e);
        }
    }
}
