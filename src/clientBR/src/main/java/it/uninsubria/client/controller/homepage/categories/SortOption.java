package it.uninsubria.client.controller.homepage.categories;

import it.uninsubria.client.utils.classesUI.LanguageManager;

public enum SortOption {
    BOOK_COUNT_DESC,
    BOOK_COUNT_ASC,
    NAME_ASC,
    NAME_DESC;

    public String getLocalizedString() {
        switch (this) {
            case BOOK_COUNT_DESC:
                return LanguageManager.getBundle().getString("categories.search.choice1");
            case BOOK_COUNT_ASC:
                return LanguageManager.getBundle().getString("categories.search.choice2");
            case NAME_ASC:
                return LanguageManager.getBundle().getString("categories.search.choice3");
            case NAME_DESC:
                return LanguageManager.getBundle().getString("categories.search.choice4");
            default:
                return "";
        }
    }
}
