package it.uninsubria.server.dao;

import java.util.List;

import it.uninsubria.shared.model.Library;

public interface LibraryDAO {

    List<Library> getUserLibraries(String username) throws Exception;
    List<Integer> getBooksInLibraries(int libreriaId) throws Exception;
    boolean libraryExistsForUser(String username, String libraryName)  throws Exception;
    Library addLibraryForUser(String username, Library library) throws Exception;
    void updateLibraryForUser(String username, Library oldLibrary, Library newLibrary) throws Exception;
    void deleteLibraryForUser(String username, Library library) throws Exception;
}
