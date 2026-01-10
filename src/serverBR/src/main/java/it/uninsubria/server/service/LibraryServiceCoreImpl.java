package it.uninsubria.server.service;

import java.util.List;
import java.util.logging.Logger;
import it.uninsubria.server.cache.CacheManager;
import it.uninsubria.server.dao.LibraryDAO;
import it.uninsubria.server.util.InputValidator;
import it.uninsubria.shared.model.Library;

public class LibraryServiceCoreImpl implements LibraryServiceCore {
    private static final Logger logger = Logger.getLogger(LibraryServiceCoreImpl.class.getName());
    private final LibraryDAO libraryDAO;

    public LibraryServiceCoreImpl(LibraryDAO libraryDAO) {
        this.libraryDAO = libraryDAO;
    }

    @Override
    public List<Library> getUserLibraries(String username) throws Exception {
        return getLibrariesFromCacheOrLoad(username);
    }

    private List<Library> getLibrariesFromCacheOrLoad(String username) throws Exception {
        String cacheKey = "libraries_" + username;
        logger.fine("Getting libraries for user '" + username + "', cache key: " + cacheKey);
        @SuppressWarnings("unchecked")
        List<Library> cached = CacheManager.get(username, cacheKey, List.class);
        if (cached != null) {
            logger.fine("Returning cached libraries, count: " + cached.size());
            return cached;
        }
        List<Library> libs = libraryDAO.getUserLibraries(username);
        logger.fine("Retrieved libraries from DAO, count: " + libs.size());
        CacheManager.put(username, cacheKey, libs, CacheManager.DEFAULT_TTL_LIBRARIES, "libraries");
        return libs;
    }

    @Override
    public List<Integer> getBooksInLibraries(int libraryId) throws Exception {
        return libraryDAO.getBooksInLibraries(libraryId);
    }

    @Override
    public boolean libraryExistsForUser(String username, String libraryName) throws Exception {
        return libraryDAO.libraryExistsForUser(username, libraryName);
    }

    @Override
    public Library addLibraryForUser(String username, Library library) throws Exception {
        InputValidator.validateUsername(username);
        if (library != null) {
            InputValidator.validateLibraryName(library.getName());
        }
        Library result = libraryDAO.addLibraryForUser(username, library);
        // Invalidate user-specific cache
        CacheManager.invalidateUserLibraries(username);
        return result;
    }

    @Override
    public void updateLibraryForUser(String username, Library oldLibrary, Library newLibrary) throws Exception {
        InputValidator.validateUsername(username);
        if (newLibrary != null) {
            InputValidator.validateLibraryName(newLibrary.getName());
        }
        libraryDAO.updateLibraryForUser(username, oldLibrary, newLibrary);
        // Invalidate user-specific cache
        CacheManager.invalidateUserLibraries(username);
    }

    @Override
    public void deleteLibraryForUser(String username, Library library) throws Exception {
        InputValidator.validateUsername(username);
        logger.fine("Deleting library '" + library.getName() + "' for user '" + username + "'");
        libraryDAO.deleteLibraryForUser(username, library);
        // Invalidate user-specific cache
        CacheManager.invalidateUserLibraries(username);
        logger.fine("Library deletion completed and cache invalidated");
    }
}
