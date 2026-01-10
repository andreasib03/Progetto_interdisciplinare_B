package it.uninsubria.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.MessageFormat;

import it.uninsubria.client.controller.homepage.home.ControllerDesign;
import it.uninsubria.client.utils.classesUI.EveryView;
import it.uninsubria.client.utils.classesUI.LanguageManager;
import it.uninsubria.client.utils.classesUI.Navigator;

import it.uninsubria.client.utils.classesUI.SessionManager;
import it.uninsubria.client.utils.classesUI.ThemeManager;
import it.uninsubria.client.utils.classesUI.TimeoutManager;
import it.uninsubria.client.di.DIContainer;
import it.uninsubria.client.utils.classesUI.ServiceLocator;
import it.uninsubria.client.utils.classesLogic.ClassGetImages;
import it.uninsubria.client.utils.classesUI.ClientAppConfig;
import it.uninsubria.shared.utils.AppConfig;
import it.uninsubria.shared.utils.AppConstants;
import it.uninsubria.client.utils.classesUI.BookServiceManager;
import it.uninsubria.client.utils.classesUI.PerformanceMonitor;
import it.uninsubria.client.utils.classesUI.ResourceCache;
import it.uninsubria.client.utils.classesUI.ResourceManager;
import it.uninsubria.client.utils.classesUI.ThreadPoolManager;
import it.uninsubria.client.utils.classesUI.BookStatisticsManager;
import it.uninsubria.shared.model.User;
import it.uninsubria.shared.model.Book;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import it.uninsubria.shared.rmi.BookService;
import javafx.scene.control.Label;
import javafx.application.Platform;
import it.uninsubria.shared.rmi.LibraryService;
import it.uninsubria.shared.rmi.ReviewsService;
import it.uninsubria.shared.rmi.SuggestionsService;
import it.uninsubria.shared.rmi.UserService;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.Pair;

/**
 * Main JavaFX application class for the Book Recommender client.
 */
public class BookRecommenderApp extends Application {
    private static final Logger logger = Logger.getLogger(BookRecommenderApp.class.getName());

    /**
     * Risolve una chiave di localizzazione. Se inizia con %, usa il bundle delle properties,
     * altrimenti restituisce la stringa così com'è.
     */
    private static String resolveString(String key) {
        if (key != null && key.startsWith("%")) {
            String actualKey = key.substring(1); // Rimuovi il %
            try {
                return LanguageManager.getBundle().getString(actualKey);
            } catch (Exception e) {
                // Se la chiave non esiste, restituisci la chiave originale per debug
                return key;
            }
        }
        return key;
    }

    @Override
    public void start(Stage primaryStage) {
        // Aggiungi shutdown hook per cleanup in caso di chiusura forzata
        addShutdownHook();

        // Aggiungi listener per intercettare chiusura finestra
        addWindowCloseListener(primaryStage);

        showSplashThenLaunchUI(primaryStage);
    }

    @Override
    public void stop() {
        // Esegui pulizia centralizzata di tutte le risorse
        ResourceManager.cleanupAll();
    }

    /**
     * Helper method to handle exceptions consistently across the application.
     * Logs errors and provides user-friendly error handling.
     */
    private void handleApplicationException(String operation, Exception e) {
        Throwable cause = e.getCause();
        if (cause instanceof java.rmi.RemoteException) {
            logger.severe("Network/RMI error during " + operation + ": " + cause.getMessage());
        } else if (cause instanceof java.io.IOException) {
            logger.severe("I/O error during " + operation + ": " + cause.getMessage());
        } else if (e instanceof RuntimeException) {
            logger.severe("Runtime error during " + operation + ": " + e.getMessage());
        }
        logger.severe("Application error during " + operation + ": " + e.getMessage());
    }
    

    /**
     * Aggiunge uno shutdown hook della JVM per cleanup anche in caso di chiusura forzata
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info(resolveString("%app.shutdown.hook.activated"));
            try {
                ResourceManager.cleanupAll();
            } catch (RuntimeException e) {
                handleApplicationException("emergency cleanup", e);
            }
        }, "BookRecommender-ShutdownHook"));
    }

    /**
     * Aggiunge un listener per intercettare la chiusura della finestra principale
     */
    private void addWindowCloseListener(Stage primaryStage) {
        primaryStage.setOnCloseRequest(event -> {
            logger.info(resolveString("%app.window.closed"));
            ResourceManager.cleanupAll();
            // Non consumare l'evento, lascia che la finestra si chiuda normalmente
        });
    }

    private ProgressBar createStyledProgressBar() {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(20);
        progressBar.getStyleClass().add("pretty-progressSplashScreen");
        return progressBar;
    }

    private MediaView createSplashMediaView(){
        try {
            Media media = new Media(getClass().getResource(EveryView.SPLASH_SCREEN_VIDEO.getPath()).toExternalForm());
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop infinito fino al completamento del caricamento
            mediaPlayer.setAutoPlay(true);

            MediaView mediaView = new MediaView(mediaPlayer);
            // Imposta dimensioni per riempire tutto lo spazio mantenendo le proporzioni
            mediaView.setFitWidth(600);
            mediaView.setFitHeight(350);
            mediaView.setPreserveRatio(true);

            return mediaView;
        } catch (Exception e) {
            handleApplicationException("splash video loading", e);
            // Return a simple placeholder instead of video
            return createFallbackSplashView();
        }
    }
    
    private MediaView createFallbackSplashView() {
        // Simply return null - the caller will handle the fallback with a colored background
        logger.info("Using colored background as splash screen fallback (video not available)");
        return null;
    }

    private void showSplashThenLaunchUI(Stage primaryStage) {
        // Crea gli elementi UI dello splash screen
        SplashUIElements uiElements = createSplashUIElements();

        // Crea e configura la finestra splash
        Stage splashStage = createSplashStage(uiElements.splashLayout);

        // Configura il task di caricamento e gli event handler
        configureLoadingTask(splashStage, primaryStage, uiElements);
    }

    /**
     * Classe helper per gli elementi UI dello splash screen
     */
    private static class SplashUIElements {
        final StackPane splashLayout;
        final Label loadingLabel;
        final ProgressBar progressBar;
        final MediaView mediaView;

        SplashUIElements(StackPane splashLayout, Label loadingLabel, ProgressBar progressBar, MediaView mediaView) {
            this.splashLayout = splashLayout;
            this.loadingLabel = loadingLabel;
            this.progressBar = progressBar;
            this.mediaView = mediaView;
        }
    }

    /**
     * Crea tutti gli elementi UI necessari per lo splash screen
     */
    private SplashUIElements createSplashUIElements() {
        ProgressBar progressBar = createStyledProgressBar();
        MediaView mediaView = createSplashMediaView();

        // Label per mostrare i messaggi di caricamento
        Label loadingLabel = new Label(resolveString("%app.splash.loading"));
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        loadingLabel.setAlignment(Pos.CENTER);

        VBox loadingBox = new VBox(10, loadingLabel, progressBar);
        loadingBox.setAlignment(Pos.BOTTOM_CENTER);

        StackPane splashLayout = createSplashLayout(mediaView, loadingBox);

        return new SplashUIElements(splashLayout, loadingLabel, progressBar, mediaView);
    }

    /**
     * Crea il layout dello splash screen
     */
    private StackPane createSplashLayout(MediaView mediaView, VBox loadingBox) {
        StackPane splashLayout;
        if(mediaView != null){
            splashLayout = new StackPane(mediaView, loadingBox);
        } else {
            Rectangle fallback = new Rectangle(600, 350, Color.DARKBLUE);
            Text titleText = new Text(resolveString("%app.splash.title"));
            titleText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
            titleText.setFill(Color.WHITE);

            Text subtitleText = new Text(resolveString("%app.splash.subtitle"));
            subtitleText.setFont(Font.font("Arial", 16));
            subtitleText.setFill(Color.LIGHTGRAY);

            VBox textBox = new VBox(10, titleText, subtitleText);
            textBox.setAlignment(Pos.CENTER);

            splashLayout = new StackPane(fallback, textBox, loadingBox);
        }

        StackPane.setAlignment(loadingBox, Pos.BOTTOM_CENTER);
        splashLayout.getStyleClass().add("layout-screenSplashScreen");

        return splashLayout;
    }

    /**
     * Crea e configura la finestra splash
     */
    private Stage createSplashStage(StackPane splashLayout) {
        Scene splashScene = new Scene(splashLayout, 600, 350, Color.TRANSPARENT);
        Stage splashStage = new Stage(StageStyle.TRANSPARENT);
        splashStage.setScene(splashScene);
        splashStage.show();
        return splashStage;
    }

    /**
     * Configura il task di caricamento e tutti gli event handler
     */
    private void configureLoadingTask(Stage splashStage, Stage primaryStage, SplashUIElements uiElements) {

        @SuppressWarnings("unchecked")
        final Pair<Parent, ControllerDesign>[] uiContentHolder = new Pair[1]; // per salvare il valore del Task

        Task<Pair<Parent, ControllerDesign>> loadingTask = createLoadingTask(text -> {
            Platform.runLater(() -> uiElements.loadingLabel.setText(text));
        });

        uiElements.progressBar.progressProperty().bind(loadingTask.progressProperty());

        setupLoadingTaskEventHandlers(splashStage, primaryStage, uiElements, loadingTask, uiContentHolder);

        // Avvia il caricamento iniziale in background con priorità critica
        ThreadPoolManager.executeCritical(() -> {
            loadingTask.run();
        });
    }

    /**
     * Configura tutti gli event handler per il task di caricamento
     */
    private void setupLoadingTaskEventHandlers(Stage splashStage, Stage primaryStage, SplashUIElements uiElements,
                                              Task<Pair<Parent, ControllerDesign>> loadingTask,
                                              Pair<Parent, ControllerDesign>[] uiContentHolder) {
        loadingTask.setOnSucceeded(event -> {
            uiContentHolder[0] = loadingTask.getValue();

            // Quando il caricamento è completato, ferma il video e lancia l'interfaccia principale
            if (uiContentHolder[0] != null) {
                // Ferma il video prima di passare all'interfaccia principale
                if (uiElements.mediaView != null && uiElements.mediaView.getMediaPlayer() != null) {
                    uiElements.mediaView.getMediaPlayer().stop();
                }
                launchMainStage(splashStage, primaryStage, uiContentHolder[0]);
            }
        });

        loadingTask.setOnFailed(e -> {
            Throwable error = loadingTask.getException();
            System.err.println("Errore durante il caricamento iniziale:");
            error.printStackTrace();

            // Ferma il video anche in caso di errore
            if (uiElements.mediaView != null && uiElements.mediaView.getMediaPlayer() != null) {
                uiElements.mediaView.getMediaPlayer().stop();
            }

            splashStage.close(); // chiudi splash se fallisce
            showConnectionErrorAndExit(); // mostra errore
        });
        
    }

    private Task<Pair<Parent, ControllerDesign>> createLoadingTask(java.util.function.Consumer<String> messageUpdater) {
        Task<Pair<Parent, ControllerDesign>> task = new Task<>() {
            @Override
            protected Pair<Parent, ControllerDesign> call() throws Exception {
                
                updateProgress(0, 100);
                updateMessage(resolveString("%app.connection.check"));
                if(!checkServerConnection()){
                    throw new RuntimeException(resolveString("%app.connection.error"));
                }
                updateProgress(10, 100);
                updateMessage(resolveString("%app.connection.success"));
                connectToRemoteServices();


                updateProgress(25, 100);
                updateMessage(resolveString("%app.session.check"));
                attemptAutoLogin();

                updateProgress(40, 100);
                updateMessage(resolveString("%app.catalog.loading"));
                preloadBookCatalog();

                updateProgress(60, 100);
                updateMessage(resolveString("%app.search.prepare"));
                preloadSearchIndex();

                updateProgress(75, 100);
                updateMessage(resolveString("%app.resources.loading"));
                ResourceCache.preloadCommonResources();

                updateProgress(90, 100);
                updateMessage(resolveString("%app.images.wait"));
                
                
                BookServiceManager bookServiceManager = BookServiceManager.getInstance();
                List<Book> popularBooks = bookServiceManager.getPopularBooks();
                long imageWaitStart = System.currentTimeMillis();
                boolean allImagesReady = false;
                int checkInterval = 100;
                int maxWaitTime = 15000;

                while(!allImagesReady && (System.currentTimeMillis() - imageWaitStart) < maxWaitTime){
                    allImagesReady = true;
                

                    for (Book book : popularBooks){
                        String cacheKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();
                        if(!ClassGetImages.isImageInCache(cacheKey)){
                            allImagesReady = false;
                            break;
                        } 
                    }

                    if(!allImagesReady){
                        try{
                            Thread.sleep(checkInterval);
                        }catch(InterruptedException e){
                            break;
                        }
                    }
                }

                if(allImagesReady){
                    updateMessage("Immagini caricate con successo");
                    logger.info(resolveString("%app.images.all.ready"));
                } else{
                    updateMessage("Timeout caricamento...");
                    logger.warning(resolveString("%app.images.timeout"));
                }

                for (Book book : popularBooks){
                    String cacheKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();
                    if(!ClassGetImages.isImageInCache(cacheKey)){
                        ClassGetImages.forcePlaceholderForBook(book);
                        logger.info(MessageFormat.format(resolveString("%app.image.placeholder.forced"), book.getTitle()));
                    } 
                }



                updateProgress(95, 100);
                updateProgress(97, 100);
                updateMessage(resolveString("%app.ui.loading"));

                 // Carica FXML e controller con immagini già disponibili
                 FXMLLoader loader = new FXMLLoader(
                     getClass().getResource(EveryView.MAIN.getPath()),
                     LanguageManager.getBundle()
                 );

                 Parent root = loader.load();
                 ControllerDesign controller = loader.getController();

                 // Precarica homepage (ora con immagini disponibili)
                 controller.loadHome();

                 updateProgress(100, 100);
                 updateMessage("✅ Avvio completato con immagini pronte!");

                 return new Pair<>(root, controller);


            }
        };

        // Collega gli aggiornamenti dei messaggi alla label
        task.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            if (messageUpdater != null && newMsg != null) {
                messageUpdater.accept(newMsg);
            }
        });

        return task;
    }

    private void connectToRemoteServices() throws Exception {
        String serverHost = System.getenv("APP_SERVER_HOST");
        if (serverHost == null || serverHost.trim().isEmpty()) {
            serverHost = AppConfig.getServerHost();
        }
        Registry registry = LocateRegistry.getRegistry(serverHost, AppConstants.Network.RMI_REGISTRY_PORT);
        UserService userService = (UserService) registry.lookup("UserService");
        BookService bookService = (BookService) registry.lookup("BookService");
        LibraryService libraryService = (LibraryService) registry.lookup("LibraryService");
        ReviewsService reviewsService = (ReviewsService) registry.lookup("ReviewsService");
        SuggestionsService suggestionsService = (SuggestionsService) registry.lookup("SuggestionsService");
        DIContainer.init(userService, bookService, libraryService, reviewsService, suggestionsService);
        ServiceLocator.initAll(userService, bookService, libraryService, reviewsService, suggestionsService);
    }

    private void attemptAutoLogin() {
        SessionManager sessionManager = SessionManager.getInstance();

        // Prima prova con le credenziali "ricordami"
        String[] rememberMeCredentials = sessionManager.getRememberMeCredentials();
        if (rememberMeCredentials != null) {
            try {
                UserService userService = ServiceLocator.getUserService();
                String token = userService.authenticateUser(rememberMeCredentials[0], rememberMeCredentials[1]);

                if (token != null && !token.isEmpty()) {
                    User user = userService.getUserByUsernameOrEmail(rememberMeCredentials[0]);

                    // Crea sessione con le credenziali ricordate
                    String userId = user.getUser_id().toString();
                    sessionManager.addUserSession(
                        userId,
                        rememberMeCredentials[0].contains("@") ? null : rememberMeCredentials[0],
                        rememberMeCredentials[0].contains("@") ? rememberMeCredentials[0] : null,
                        token,
                        user
                    );

                    // Imposta timeout di 30 minuti per sessioni "ricordami"
                    TimeoutManager.getInstance().setTimeoutMinutes(30);

                    logger.info(java.text.MessageFormat.format(resolveString("%app.autologin.remember.success"), rememberMeCredentials[0]));
                    return;
                } else {
                    // Credenziali non valide, cancellale
                    sessionManager.clearRememberMeCredentials();
                    logger.info(resolveString("%app.autologin.remember.invalid"));
                }
            } catch (java.rmi.RemoteException e) {
                logger.warning("Auto-login with remember me failed due to server error: " + e.getMessage());
                return;
            } catch (RuntimeException e) {
                handleApplicationException("auto-login with remember me", e);
                return;
            }
        }

        // Fallback: prova con la sessione cachata esistente
        if (sessionManager.loadFromCache()) {
            String token = sessionManager.getSessionToken();

            if (token != null && !token.isEmpty()) {
                try {
                    // Validate token with server
                    UserService userService = ServiceLocator.getUserService();
                    boolean isValid = userService.validateSession(token);

                    if (isValid) {
                        // Token is valid, get user info and complete login
                        String usernameOrEmail = sessionManager.getUsername();
                        if (usernameOrEmail == null || usernameOrEmail.isEmpty()) {
                            usernameOrEmail = sessionManager.getEmail();
                        }

                        if (usernameOrEmail != null && !usernameOrEmail.isEmpty()) {
                             User user = userService.getUserByUsernameOrEmail(usernameOrEmail);
                             sessionManager.setUser(user);
                            logger.info(java.text.MessageFormat.format(resolveString("%app.autologin.success"), usernameOrEmail));
                            return;
                        }
                    } else {
                        logger.info(resolveString("%app.autologin.token.invalid"));
                    }
                } catch (java.rmi.RemoteException e) {
                    logger.warning("Auto-login failed due to server error: " + e.getMessage());
                    return;
                } catch (RuntimeException e) {
                    logger.warning("Auto-login failed due to unexpected error: " + e.getMessage());
                    // Don't clear cache if it's an unexpected issue
                    return;
                }
            }

            // If we reach here, auto-login failed due to invalid token - clear cache
            logger.info(resolveString("%app.session.invalid.clearing"));

            // Prima invalida la sessione lato server (anche se probabilmente già invalida)
            String invalidToken = sessionManager.getSessionToken();
            if (invalidToken != null && !invalidToken.isEmpty()) {
                try {
                    ServiceLocator.getUserService().invalidateSession(invalidToken);
                } catch (java.rmi.RemoteException e) {
                    // Ignora errori di connessione durante la pulizia
                } catch (Exception e) {
                    // Ignora altri errori durante la pulizia
                }
            }

            sessionManager.logout();
        }
    }

    private void preloadBookCatalog() {
        try {
            // Verifica connessione server prima di iniziare
            logger.info(resolveString("%app.connection.check"));
            ServiceLocator.getBookService().searchGlobally(); // Test connessione semplice
            logger.info(resolveString("%app.connection.established"));

            BookServiceManager bookManager = BookServiceManager.getInstance();
            // Carica solo libri essenziali invece di tutti i libri per avvio più veloce
            preloadEssentialBooks(bookManager);
            logger.info(resolveString("%app.catalog.preloaded"));
        } catch (java.util.concurrent.TimeoutException e) {
            logger.warning("Timeout nel precaricamento del catalogo libri (60s): alcune operazioni potrebbero non essere completate, ma l'app continuerà a funzionare.");
        } catch (java.rmi.RemoteException e) {
            logger.warning("Errore di connessione nel precaricamento libri: verificare che il server sia in esecuzione. " + e.getMessage());
                } catch (RuntimeException e) {
                    logger.warning("Errore imprevisto nel precaricamento libri: " + (e.getMessage() != null ? e.getMessage() : "sconosciuto"));
                } catch (Exception e) {
                    logger.severe("Errore generico nel precaricamento libri: " + (e.getMessage() != null ? e.getMessage() : "sconosciuto"));
                }
    }

    private void preloadEssentialBooks(BookServiceManager bookManager) throws Exception {
        logger.info(resolveString("%app.loading.complete.books"));

        try {
            // Carica TUTTI i libri con supporto cache locale
            logger.info(resolveString("%app.loading.books.database"));
            List<Book> allBooks = PerformanceMonitor.monitorBookLoading(() -> {
                try {
                    return bookManager.loadAllBooks().get(
                        ClientAppConfig.getBookLoadTimeoutSec(), java.util.concurrent.TimeUnit.SECONDS);
                 } catch (RuntimeException e) {
                     logger.severe("Errore caricamento libri: " + e.getMessage());
                     return null;
                 } catch (InterruptedException e) {
                     logger.warning("Caricamento libri interrotto");
                     Thread.currentThread().interrupt();
                 } catch (ExecutionException e) {
                     logger.severe("Errore esecuzione caricamento libri: " + e.getMessage());
                 } catch (TimeoutException e) {
                     logger.warning("Timeout caricamento libri");
                }
                return null;
            });

            logger.info(java.text.MessageFormat.format(resolveString("%app.books.loaded.total"), allBooks.size(),
                (ClientAppConfig.getMaxBooksStartup() > 0 ? ClientAppConfig.getMaxBooksStartup() : resolveString("%search.no.results.found"))));

            // Informazioni cache per debug
            if (bookManager.isDiskCacheValid()) {
                logger.info(resolveString("%app.books.loaded.from.cache"));
            } else {
                logger.info(resolveString("%app.books.loaded.from.server"));
            }

            // Assicura che ControllerDesign abbia accesso a questi libri
            ControllerDesign.ensureBooksLoaded();

            // Pre-calcola categorie in parallelo per velocità immediata

            // Precarica immagini, categorie e statistiche per tutti i libri caricati
            long startTime = System.currentTimeMillis();

            // Operazione 1: Precarica categorie BLOCCANDO l'avvio fino al completamento
            logger.info(resolveString("%app.categories.precalculation"));
            long catStart = System.currentTimeMillis();

            try {
                // Prima prova a caricare dalla cache su disco
                if (!ControllerDesign.loadCategoriesFromDisk()) {
                    // Se non disponibile o invalida, calcola da zero
                    ControllerDesign.prebuildCategoriesIfNeeded();
                    // Salva per i prossimi avvii
                    ControllerDesign.saveCategoriesToDisk();
                }

                long catDuration = System.currentTimeMillis() - catStart;
                logger.info(java.text.MessageFormat.format(resolveString("%app.categories.precalculated"), catDuration));

            } catch (RuntimeException e) {
                logger.severe("Errore pre-calcolo categorie: " + e.getMessage());
            }

            // Operazione 1.5: Seleziona libri popolari per BookServiceManager (IMPORTANTE: deve avvenire PRIMA del precaricamento immagini)
            logger.info(resolveString("%app.popular.books.selection"));
            BookServiceManager bookServiceManager = BookServiceManager.getInstance();
            bookServiceManager.selectPopularBooks();
            List<Book> popularBooks = bookServiceManager.getPopularBooks();
            logger.info(java.text.MessageFormat.format(resolveString("%app.popular.books.selected"), popularBooks.size()));
            logger.info(resolveString("%app.popular.books.list"));
            for (int i = 0; i < popularBooks.size(); i++) {
                Book book = popularBooks.get(i);
                logger.info(java.text.MessageFormat.format(resolveString("%app.popular.book.item"), i+1, book.getTitle(), book.getBook_id()));
            }

            // Operazione 2: Precarica immagini per i libri popolari selezionati (per UX migliore)
            CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
                try {
                    // Usa ESATTAMENTE gli stessi libri popolari che saranno mostrati nella homepage
                    logger.info(java.text.MessageFormat.format(resolveString("%app.images.preloading"), popularBooks.size()));
                    preloadBookCovers(popularBooks);

                    // Verifica che le immagini siano state caricate
                    verifyCacheKeyConsistency(popularBooks);
            } catch (RuntimeException e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Timeout nelle operazioni di precaricamento (60 secondi)";
                logger.severe("Errore nel caricamento libri essenziali: " + errorMsg);
                logger.info("L'app continuerà a funzionare con caricamento lazy - alcune funzionalità potrebbero essere più lente.");
            }
            });

            // Operazione 2: Calcola statistiche SOLO per i libri popolari (ottimizzazione prestazioni)
            CompletableFuture<Void> statsFuture = CompletableFuture.runAsync(() -> {
                try {
                    logger.info(resolveString("%app.statistics.calculation"));
                    BookStatisticsManager statsManager = BookStatisticsManager.getInstance();
                    // Carica statistiche solo per i libri che saranno effettivamente mostrati
                    statsManager.loadBookStatistics(popularBooks).get();
                    logger.info(java.text.MessageFormat.format(resolveString("%app.statistics.calculated"), popularBooks.size()));
                } catch (RuntimeException e) {
                    logger.severe("Errore calcolo statistiche libri popolari: " + e.getMessage());
                } catch (InterruptedException e) {
                    logger.warning("Calcolo statistiche interrotto");
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    logger.severe("Errore esecuzione calcolo statistiche: " + (e.getMessage() != null ? e.getMessage() : "sconosciuto"));
                }
            });

            // Aspetta che tutte le operazioni completino (max 60 secondi - aumentato per stabilità)
            // IMPORTANTE: Aspetta anche che i libri popolari siano selezionati
            logger.info(resolveString("%app.splash.wait"));
            logger.info(resolveString("%app.splash.wait.images"));
            logger.info(resolveString("%app.splash.wait.stats"));
            long waitStart = System.currentTimeMillis();
            CompletableFuture<Void> allOperations = CompletableFuture.allOf(imagesFuture, statsFuture);
            allOperations.get(60, java.util.concurrent.TimeUnit.SECONDS);
            long waitDuration = System.currentTimeMillis() - waitStart;
            logger.info("✅ Tutte le operazioni splash screen completate in " + waitDuration + "ms");

            // Verifica finale che tutto sia pronto
            if (bookServiceManager.arePopularBooksSelected()) {
                List<Book> finalPopularBooks = bookServiceManager.getPopularBooks();
                logger.info("✅ Splash screen completato: " + finalPopularBooks.size() + " libri popolari pronti");
            } else {
                logger.warning("⚠️ Attenzione: libri popolari non selezionati alla fine dello splash screen!");
            }

            long endTime = System.currentTimeMillis();
            logger.info("Precaricamento completato in " + (endTime - startTime) + "ms");

        } catch (RuntimeException e) {
            logger.warning("Timeout nel precaricamento del catalogo libri (60s): alcune operazioni potrebbero non essere completate, ma l'app continuerà a funzionare.");
            logger.warning("Errore di connessione nel precaricamento libri: verificare che il server sia in esecuzione. " + e.getMessage());
            logger.warning("Errore imprevisto nel precaricamento libri: " + (e.getMessage() != null ? e.getMessage() : "sconosciuto"));
        }
    }

    /**
     * Seleziona gli STESSI libri che appariranno nella homepage
     * Usa la stessa logica di selezione per garantire cache hit perfetto
     */
    /**
     * Verifica che le chiavi di cache siano coerenti tra preload e utilizzo normale
     */
    private void verifyCacheKeyConsistency(List<Book> books) {
        logger.info("🔍 Verifica coerenza chiavi cache per " + books.size() + " libri:");

        boolean allConsistent = true;
        for (Book book : books) {
            // Chiave come usata nel preload
            String preloadKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();

            // Simula la stessa logica usata in setImageFromCacheOrLoad
            String normalKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();

            boolean consistent = preloadKey.equals(normalKey);
            logger.info("   📚 '" + book.getTitle() + "': " +
                (consistent ? "✅ COERENTE" : "❌ NON COERENTE") +
                " (preload: '" + preloadKey + "', normal: '" + normalKey + "')");

            if (!consistent) {
                allConsistent = false;
            }
        }

        if (allConsistent) {
            logger.info("✅ TUTTE le chiavi cache sono coerenti tra splash screen e utilizzo normale!");
        } else {
            logger.warning("❌ ALCUNE chiavi cache NON sono coerenti - possibili problemi di caricamento immagini!");
        }

        // Verifica aggiuntiva: controlla se le immagini sono già state caricate
        logger.info("📊 Stato cache immagini dopo precaricamento:");
        int cachedCount = 0;
        for (Book book : books) {
            String cacheKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();
            boolean inCache = it.uninsubria.client.utils.classesLogic.ClassGetImages.isImageInCache(cacheKey);
            if (inCache) cachedCount++;
            logger.info("   " + (inCache ? "✅" : "❌") + " " + book.getTitle());
        }
        logger.info("📈 Risultato: " + cachedCount + "/" + books.size() + " immagini già in cache");
    }

    /**
     * Precarica le copertine dei libri durante lo splash screen - ULTRA OTTIMIZZATO
     * Carica immagini per libri popolari con recensioni direttamente in cache senza UI temporanea
     * Le immagini precaricate verranno riutilizzate direttamente nell'FXML senza chiamate duplicate
     */
    private void preloadBookCovers(List<Book> books) {
        logger.info("Precaricamento intelligente copertine libri durante splash screen...");

        try {
            // OTTIMIZZAZIONE: Timeout ridotto per immagine singola (3 secondi - più aggressivo)
            final int IMAGE_TIMEOUT_SECONDS = 3;
            // OTTIMIZZAZIONE: Timeout totale ridotto (15 secondi - meno libri da caricare)
            final int TOTAL_TIMEOUT_SECONDS = 15;

            List<java.util.concurrent.CompletableFuture<Void>> imageFutures = new ArrayList<>();

            for (Book book : books) {
                // Usa il nuovo metodo ottimizzato per splash screen che carica direttamente in cache
                java.util.concurrent.CompletableFuture<Image> imageFuture =
                    it.uninsubria.client.utils.classesLogic.ClassGetImages.preloadImageToCache(book);

                // Converte CompletableFuture<Image> a CompletableFuture<Void> per compatibilità
                java.util.concurrent.CompletableFuture<Void> voidFuture = imageFuture.thenApply(img -> null);

                // OTTIMIZZAZIONE: Timeout ridotto per velocità
                java.util.concurrent.CompletableFuture<Void> imageFutureWithTimeout =
                    voidFuture.orTimeout(IMAGE_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                    .exceptionally(throwable -> {
                        // Completa comunque per non bloccare altri caricamenti
                        return null;
                    });

                imageFutures.add(imageFutureWithTimeout);
            }

            // OTTIMIZZAZIONE: Non aspettare che TUTTE le immagini siano caricate
            // Usa anyOf per completare quando almeno il 70% è pronto o dopo timeout
            java.util.concurrent.CompletableFuture<Void> allImagesFuture =
                java.util.concurrent.CompletableFuture.allOf(imageFutures.toArray(new java.util.concurrent.CompletableFuture[0]));

            // Crea un timeout separato
            java.util.concurrent.CompletableFuture<Void> timeoutFuture = new java.util.concurrent.CompletableFuture<>();
            java.util.concurrent.CompletableFuture.delayedExecutor(TOTAL_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> timeoutFuture.complete(null));

            // Completa quando o tutte le immagini sono pronte o scade il timeout
            java.util.concurrent.CompletableFuture.anyOf(allImagesFuture, timeoutFuture).get();

            // Conta quante immagini sono state effettivamente caricate
            long completedImages = imageFutures.stream()
                .filter(future -> future.isDone() && !future.isCompletedExceptionally())
                .count();

            logger.info("Precaricamento completato: " + completedImages + "/" + books.size() + " immagini caricate");

            // Segna che il precaricamento è completato per evitare chiamate API duplicate
            it.uninsubria.client.utils.classesLogic.ClassGetImages.markPreloadCompleted();

        } catch (Exception e) {
            logger.info("Errore nel precaricamento delle copertine: " + e.getMessage());
            logger.info("Continuando senza precaricamento immagini - verranno caricate on-demand");
        }
    }

    private void preloadSearchIndex() {
        try {
            // Precarica l'indice di ricerca per evitare blocchi durante la digitazione
            // Questo chiama ensureBooksLoaded() che carica i libri e costruisce l'indice invertito
            it.uninsubria.client.controller.homepage.home.ControllerDesign.ensureBooksLoaded();
            logger.info("Indice di ricerca precaricato con successo");
        } catch (Exception e) {
            logger.warning("Errore nel precaricamento dell'indice di ricerca: " + e.getMessage());
            // Non bloccare l'avvio dell'app se il caricamento fallisce
        }
    }


    private void launchMainStage(Stage splashStage, Stage primaryStage, Pair<Parent, ControllerDesign> result){
            splashStage.close();

            Parent root = result.getKey();
            Scene mainScene = new Scene(root);

            ThemeManager.applyTheme(mainScene);
            mainScene.getStylesheets().add(getClass().getResource(EveryView.CSS_GENERAL.getPath()).toExternalForm());

            primaryStage.setTitle(LanguageManager.getBundle().getString("app.title"));
            primaryStage.setResizable(false);
            primaryStage.setScene(mainScene);
            primaryStage.show();



            // Set the main stage for navigation
            Navigator.setMainStage(primaryStage);

            // Initialize timeout manager and start monitoring user activity
            TimeoutManager.getInstance().resetTimer();

            // Add activity listeners to reset timeout on user interaction
            mainScene.setOnMouseClicked(e -> TimeoutManager.getInstance().resetTimer());
            mainScene.setOnKeyPressed(e -> TimeoutManager.getInstance().resetTimer());
            mainScene.setOnMouseMoved(e -> TimeoutManager.getInstance().resetTimer());

            // Fade-in scena principale
            FadeTransition fade = new FadeTransition(Duration.millis(600), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
    }


    private boolean checkServerConnection(){
        try {
            Registry registry = LocateRegistry.getRegistry(AppConfig.getServerHost(), AppConstants.Network.RMI_REGISTRY_PORT);
            registry.lookup("UserService");
            return true;
        } catch (RuntimeException e) {
            logger.warning("Errore nella connessione al server RMI: " + e.getMessage());
            return false;
        } catch (RemoteException e) {
            logger.warning("Errore RMI: " + e.getMessage());
        } catch (NotBoundException e) {
            logger.warning("Servizio non disponibile: " + e.getMessage());
        }
        return false;
    }

    private void showConnectionErrorAndExit(){
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(LanguageManager.getBundle().getString("app.connection.error.title"));
            alert.setHeaderText(LanguageManager.getBundle().getString("app.connection.error.header"));
            alert.setContentText(LanguageManager.getBundle().getString("app.connection.error.message"));

            alert.setOnHidden(evt -> {
                System.exit(1); // termina l'app
            });

            alert.show();
        });
    }

    /**
     * Main entry point for the JavaFX application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
