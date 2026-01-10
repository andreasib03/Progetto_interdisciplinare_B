package it.uninsubria.server.dao;

import java.util.ArrayList;
import java.util.List;

import it.uninsubria.shared.model.Library;

public class TestLibraryDAO implements LibraryDAO {
    private int getUserLibrariesCalls = 0;
    private int getBooksInLibrariesCalls = 0;
    private int libraryExistsForUserCalls = 0;
    private int addLibraryForUserCalls = 0;
    private int updateLibraryForUserCalls = 0;
    private int deleteLibraryForUserCalls = 0;

    @Override
    public List<Library> getUserLibraries(String username) {
        getUserLibrariesCalls++;
        List<Library> list = new ArrayList<>();
        list.add(new Library(1, username, "TestLib"));
        return list;
    }

    @Override
    public List<Integer> getBooksInLibraries(int libraryId) {
        getBooksInLibrariesCalls++;
        List<Integer> l = new ArrayList<>();
        l.add(1);
        l.add(2);
        return l;
    }

    @Override
    public boolean libraryExistsForUser(String username, String libraryName) {
        libraryExistsForUserCalls++;
        return true;
    }

    @Override
    public Library addLibraryForUser(String username, Library library) {
        addLibraryForUserCalls++;
        return library;
    }

    @Override
    public void updateLibraryForUser(String username, Library oldLibrary, Library newLibrary) {
        updateLibraryForUserCalls++;
    }

    @Override
    public void deleteLibraryForUser(String username, Library library) {
        deleteLibraryForUserCalls++;
    }

    // Getters for test assertions
    public int getGetUserLibrariesCalls() { return getUserLibrariesCalls; }
    public int getGetBooksInLibrariesCalls() { return getBooksInLibrariesCalls; }
    public int getLibraryExistsForUserCalls() { return libraryExistsForUserCalls; }
    public int getAddLibraryForUserCalls() { return addLibraryForUserCalls; }
    public int getUpdateLibraryForUserCalls() { return updateLibraryForUserCalls; }
    public int getDeleteLibraryForUserCalls() { return deleteLibraryForUserCalls; }
}
