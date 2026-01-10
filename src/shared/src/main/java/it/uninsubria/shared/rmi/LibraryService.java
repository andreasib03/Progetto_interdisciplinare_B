package it.uninsubria.shared.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import it.uninsubria.shared.model.Library;

public interface LibraryService extends Remote {
    List<Library> getUserLibraries(String username) throws RemoteException;
    List<Integer> getBooksInLibrariesImpl(int idLibreria) throws RemoteException;
    boolean libraryExistsForUser(String username, String libraryName) throws RemoteException;
    Library addLibraryForUser(String username, Library library) throws RemoteException;
    void updateLibraryForUser(String username, Library oldLibrary, Library newLibrary) throws RemoteException;
    void deleteLibraryForUser(String username, Library library) throws RemoteException;

}
