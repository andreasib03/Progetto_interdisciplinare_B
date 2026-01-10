package it.uninsubria.server.service;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import it.uninsubria.server.dao.TestLibraryDAO;
import it.uninsubria.shared.model.Library;

public class LibraryServiceCoreTest {

    @Test
    public void testCachingGetUserLibraries() throws Exception {
        TestLibraryDAO dao = new TestLibraryDAO();
        LibraryServiceCore core = new LibraryServiceCoreImpl(dao);
        it.uninsubria.server.cache.CacheManager.clear();
        List<Library> libs1 = core.getUserLibraries("alice");
        assertNotNull(libs1);
        assertEquals(1, libs1.size());
        assertEquals(1, dao.getGetUserLibrariesCalls());
        List<Library> libs2 = core.getUserLibraries("alice");
        assertEquals(1, dao.getGetUserLibrariesCalls()); // from cache
        assertEquals(libs1, libs2);
    }

    @Test
    public void testOtherDelegations() throws Exception {
        TestLibraryDAO dao = new TestLibraryDAO();
        LibraryServiceCore core = new LibraryServiceCoreImpl(dao);
        core.getBooksInLibraries(1);
        assertEquals(1, dao.getGetBooksInLibrariesCalls());
        // libraryExistsForUser delegation
        assertTrue(core.libraryExistsForUser("u", "lib"));
    }
}
