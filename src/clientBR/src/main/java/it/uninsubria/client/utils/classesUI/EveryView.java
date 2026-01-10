package it.uninsubria.client.utils.classesUI;

public enum EveryView {
    MAIN("/fxml/homepageDesign.fxml"),
    LOGIN("/fxml/Login_Reformed.fxml"),
    REGISTRATION("/fxml/Registration.fxml"),
    FORGOT_PASSWORD("/fxml/ForgotPassword.fxml"),

    HOME_VIEW("/fxml/views/viewsHome.fxml"),
    LOADING_VIEW("/fxml/LoadingView.fxml"),
    SETTINGS_VIEW("/fxml/views/viewsSettings.fxml"),
    LIBRARIES_VIEW("/fxml/views/viewsLibraries.fxml"),
    CARD_VIEW("/fxml/views/library/library_card.fxml"),
    CATEGORIES_VIEW("/fxml/views/viewsCategories.fxml"),
    CATEGORY_DETAIL_VIEW("/fxml/views/viewsCategoryDetail.fxml"),
    ABOUT_VIEW("/fxml/views/viewsAbout.fxml"),
    FORM_CONTACT("/fxml/views/contact_form.fxml"),
    EXIT_VIEW("/fxml/views/exit.fxml"),

    WINDOW("/fxml/views/settings/viewWindowSettings.fxml"),
    MODIFY("/fxml/views/settings/viewModifyProfile.fxml"),
    CHANGE("/fxml/views/settings/viewChangePassword.fxml"),
    FAQ_VIEW("/fxml/views/settings/faq_view.fxml"),
    VIDEO_VIEW("/fxml/views/settings/video_view.fxml"),

    RATING_VIEW("/fxml/views/library/ValutazioneView.fxml"),

    PDF_MANUAL_USER("/pdf/Manuale_utente.pdf"),
    PDF_MANUAL_TECNICAL("/pdf/Manuale_tecnico.pdf"),

    CSS_GENERAL("/css/CommonFeatures.css"),
    CSS_DARKMODE("/css/DarkMode.css"),
    CSS_LIGHTMODE("/css/LightMode.css"),


    SPLASH_SCREEN_VIDEO("/video/Splash-screen.mp4"),

    TEST_IMAGE("/images/logo_black.png"),
    LIBRARY_DETAIL_VIEW("/fxml/views/library/library_detail.fxml"),
    CREATE_LIBRARY_VIEW("/fxml/views/library/CreateLibraryView.fxml"),

    SUGGESTIONS_DETAIL_VIEW("/fxml/views/my_suggestions.fxml"),
    ALL_REVIEWS("/fxml/views/AllReviewsView.fxml"),
    BOOK_DETAIL_SIMPLE("/fxml/views/BookDetailSimpleView.fxml");


    // ...

    private final String path;
    
    EveryView(String path) { 
        this.path = path; 
    }

    public String getPath() { 
        return path;
    }
}
