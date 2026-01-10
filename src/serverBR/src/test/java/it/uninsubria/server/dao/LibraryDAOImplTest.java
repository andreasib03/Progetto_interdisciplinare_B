package it.uninsubria.server.dao;

import org.junit.Test;

import java.util.ArrayList;
import java.sql.Connection;

import it.uninsubria.shared.model.Library;
import it.uninsubria.shared.model.Book;
import it.uninsubria.server.dao.impl.LibraryDAOImpl;


public class LibraryDAOImplTest {

    @Test(expected = IllegalArgumentException.class)
    public void getUserLibraries_shouldValidateUsername_null() throws Exception {
        LibraryDAO dao = new LibraryDAOImpl((Connection) null);
        dao.getUserLibraries(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void libraryExistsForUser_shouldValidateUsername_empty() throws Exception {
        LibraryDAO dao = new LibraryDAOImpl((Connection) null);
        dao.libraryExistsForUser("", "Test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addLibraryForUser_shouldValidateUsername_null() throws Exception {
        Library lib = new Library("TestLib", new ArrayList<Book>());
        LibraryDAO dao = new LibraryDAOImpl((Connection) null);
        dao.addLibraryForUser(null, lib);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateLibraryForUser_shouldValidateUsername_null() throws Exception {
        Library oldLib = new Library(1, "user", "Old");
        Library newLib = new Library(1, "user", "New");
        LibraryDAO dao = new LibraryDAOImpl((Connection) null);
        dao.updateLibraryForUser(null, oldLib, newLib);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateLibraryForUser_shouldValidateNullOldLibrary() throws Exception {
        Library newLib = new Library(1, "user", "New");
        LibraryDAO dao = new LibraryDAOImpl((Connection) null);
        dao.updateLibraryForUser("user", null, newLib);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteLibraryForUser_shouldValidateUsername_null() throws Exception {
        Library lib = new Library(1, "user", "Lib");
        LibraryDAO dao = new LibraryDAOImpl((Connection) null);
        dao.deleteLibraryForUser(null, lib);
    }

    // Nuovi test per ampliamento copertura (T11)
    @Test(expected = IllegalArgumentException.class)
    public void libraryExistsForUser_shouldValidateLibraryName_tooLong() throws Exception {
        LibraryDAO dao = new LibraryDAOImpl((Connection) null);
        String longName = "L".repeat(101);
        dao.libraryExistsForUser("user", longName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addLibraryForUser_shouldValidateBooksNull() throws Exception {
        Library lib = new Library("TestLib", null);
        LibraryDAO dao = new LibraryDAOImpl((Connection) null);
        dao.addLibraryForUser("user", lib);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateLibraryForUser_shouldValidateNewLibraryNull() throws Exception {
        Library oldLib = new Library(1, "user", "Old");
        Library newLib = null;
        LibraryDAO dao = new LibraryDAOImpl((Connection) null);
        dao.updateLibraryForUser("user", oldLib, newLib);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteLibraryForUser_shouldValidateUsername_empty() throws Exception {
        Library lib = new Library(1, "user", "Lib");
        LibraryDAO dao = new LibraryDAOImpl((Connection) null);
        dao.deleteLibraryForUser("", lib);
    }
}
