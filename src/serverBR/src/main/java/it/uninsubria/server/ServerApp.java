 package it.uninsubria.server;

 import it.uninsubria.server.db.BookRecommenderServer;
 import java.util.logging.Logger;
 import javafx.application.Application;
 import javafx.fxml.FXMLLoader;
 import javafx.scene.Parent;
 import javafx.scene.Scene;
 import javafx.stage.Stage;

 public class ServerApp extends Application {

     private static final Logger logger = Logger.getLogger(ServerApp.class.getName());

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/server.fxml"));
        Parent root = loader.load();
        // existing server GUI path
        primaryStage.setTitle("Book Recommender - Server");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);

        primaryStage.setOnCloseRequest(event -> {
            logger.info("Chiusura server in corso...");

            try {
                it.uninsubria.server.util.ConnectionPoolManager.shutdown();
                logger.info("Pool di connessioni chiuso");
                it.uninsubria.server.db.BookRecommenderServer.stopServer();
                logger.info("Server RMI fermato");
            } catch (Exception e) {
                logger.severe("Errore durante la chiusura del server: " + e.getMessage());
            }

            logger.info("Server fermato correttamente");
            System.exit(0);
        });

        primaryStage.show();
    }

    private static void bootstrapHeadless() {
        try {
            it.uninsubria.server.monitoring.MonitoringService.getInstance();
            logger.info("Monitoring service initialized");

            // Load database configuration from environment variables for security
            String host = System.getenv("DB_HOST");
            if (host == null || host.trim().isEmpty()) {
                host = "localhost"; // fallback for development
            }

            String dbName = System.getenv("DB_NAME");
            if (dbName == null || dbName.trim().isEmpty()) {
                dbName = "projectb"; // fallback for development
            }

            String user = System.getenv("DB_USER");
            if (user == null || user.trim().isEmpty()) {
                throw new RuntimeException("DB_USER environment variable is required");
            }

            String password = System.getenv("DB_PASSWORD");
            if (password == null || password.trim().isEmpty()) {
                throw new RuntimeException("DB_PASSWORD environment variable is required");
            }

            it.uninsubria.server.db.DBInitializer.initialize(host, dbName, user, password);
            BookRecommenderServer.startServer();

            // Initialize cache warm-up after server is ready
            try {
                it.uninsubria.server.cache.CacheWarmUpService.warmUpCache(
                    it.uninsubria.server.di.ServerDIContainer.getBookCore()
                );
            } catch (Exception e) {
                logger.warning("Cache warm-up failed to initialize: " + e.getMessage());
            }

            logger.info("Headless server started with enhanced monitoring and caching. RMI registry on port 1099.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down services...");
                it.uninsubria.server.cache.HybridCacheManager.shutdown();
                it.uninsubria.server.cache.CacheWarmUpService.shutdown();
                try {
                    BookRecommenderServer.stopServer();
                } catch (Exception e) {
                    logger.severe("Error stopping server: " + e.getMessage());
                }
                logger.info("Services shutdown completed");
            }));

            // Keep the server running
            while (true) {
                Thread.sleep(1000);
            }
            } catch (Exception e) {
                logger.severe("Server startup failed: " + e.getMessage());
                System.exit(1);
            }
    }

    public static void main(String[] args) {
        boolean headless = false;
        for (String a : args) if (a.equalsIgnoreCase("--headless")) headless = true;
        String prop = System.getProperty("headless", "false");
        if (prop.equalsIgnoreCase("true")) headless = true;
        if (headless) {
            bootstrapHeadless();
        } else {
            launch(args);
        }
    }
}
