package it.uninsubria.client.utils.classesLogic;

import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

import it.uninsubria.shared.model.Book;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ClassGetImages {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ClassGetImages.class.getName());
    private static final int IMAGE_CACHE_CAP = 200;
    private static final Map<String, Image> imageCache = new LinkedHashMap<String, Image>(IMAGE_CACHE_CAP, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > IMAGE_CACHE_CAP;
        }
    };

    // Flag per indicare se il precaricamento dello splash screen è completato
    private static boolean preloadCompleted = false;

    // Flag per indicare se abbiamo già verificato la connettività internet
    private static boolean connectivityChecked = false;
    private static boolean hasInternetConnection = true;

    // Timeout ridotto per dispositivi con connessione lenta o assente
    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration IMAGE_TIMEOUT = Duration.ofSeconds(8);

    /**
     * Verifica se il precaricamento delle immagini è stato completato
     */
    public static boolean isPreloadCompleted() {
        return preloadCompleted;
    }

    // Percorso immagine placeholder (modifica se vuoi)
    public static final String PLACEHOLDER_IMAGE = ClassGetImages.class.getResource("/images/placeholder_image.png").toExternalForm();

     /**
      * Verifica se c'è connessione internet disponibile (metodo pubblico per UI)
      * Effettua un test rapido con timeout ridotto
      */
     public static boolean checkInternetConnectivityForUI() {
         return checkInternetConnectivity();
     }

     /**
      * Verifica se c'è connessione internet disponibile
      * Effettua un test rapido con timeout ridotto
      */
     private static boolean checkInternetConnectivity() {
        if (connectivityChecked) {
            return hasInternetConnection;
        }

        connectivityChecked = true;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://httpbin.org/status/200"))
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            hasInternetConnection = (response.statusCode() == 200);
            logger.info("🌐 Controllo connettività: " + (hasInternetConnection ? "ONLINE" : "OFFLINE"));

        } catch (Exception e) {
            hasInternetConnection = false;
            logger.info("🌐 Connessione internet non disponibile: " + e.getMessage());
        }

        return hasInternetConnection;
    }

    /**
     * Segna che il precaricamento delle immagini è stato completato
     * Usato principalmente per logging e statistiche
     */
    public static void markPreloadCompleted() {
        preloadCompleted = true;

        // Logga statistiche complete cache
        var diskStats = ImageDiskCache.getCacheStats();
        logger.info("✅ Precaricamento immagini completato:");
        logger.info("   📱 Cache memoria: " + imageCache.size() + " immagini");
        logger.info("   💾 Cache disco: " + diskStats);

        // Pulizia periodica cache disco (ogni avvio)
        ImageDiskCache.cleanupOldImageUrls();
    }

    public static void findCoverImage(String bookTitle, String author, int publishYear, ImageView imageViewBase, Consumer<Image> onImageLoaded) {
        String cacheKey =  bookTitle + "_" + author + "_" + publishYear;

        // 🎯 PRIMA: Controlla cache memoria (velocissima)
        if (imageCache.containsKey(cacheKey)) {
            Image img = imageCache.get(cacheKey);
            logger.info("✅ Cache hit memoria per: " + cacheKey);
            Platform.runLater(() -> {
                imageViewBase.setImage(img);
                if (onImageLoaded != null)
                    onImageLoaded.accept(img);
            });
            return;
        }

        // 🐛 DEBUG: Log quando non trova in cache memoria
        logger.info("❌ Cache miss memoria per: " + cacheKey + " - Cerco online...");

        // 🌐 SECONDA: Controlla connettività internet prima di tentare API
        if (!checkInternetConnectivity()) {
            logger.info("🚫 Connessione internet assente, uso placeholder per: " + cacheKey);
            setPlaceholderImage(imageViewBase, onImageLoaded, cacheKey);
            return;
        }

        // 🎯 SECONDA: Controlla cache disco persistente (molto veloce)
        String cachedImageUrl = ImageDiskCache.loadImageUrlFromDisk(cacheKey);
        if (cachedImageUrl != null) {
            try {
                Image img = new Image(cachedImageUrl, true); // Async loading
                img.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                    if (newProgress.doubleValue() >= 1.0 && !img.isError()) {
                        // Immagine caricata con successo dalla cache disco
                        imageCache.put(cacheKey, img); // Salva anche in memoria
                        logger.info("💾 Immagine caricata da cache disco e salvata in memoria: " + cacheKey);
                        Platform.runLater(() -> {
                            imageViewBase.setImage(img);
                            if (onImageLoaded != null)
                                onImageLoaded.accept(img);
                        });
                    } else if (img.isError()) {
                        // Errore nel caricamento dell'immagine dalla cache disco
                        logger.info("⚠️ URL cache disco corrotta per: " + cacheKey + ", procedo con ricerca API");
                        // Procedi con ricerca API invece di fermarti
                        searchWithParams(bookTitle, author, publishYear, imageViewBase, onImageLoaded, cacheKey, 1);
                    }
                });

                // Aggiungi timeout per evitare blocchi prolungati
                final ImageView finalImageView = imageViewBase;
                final Consumer<Image> finalOnImageLoaded = onImageLoaded;
                Platform.runLater(() -> {
                    new Thread(() -> {
                        try {
                            Thread.sleep(IMAGE_TIMEOUT.toMillis());
                            if (img.getProgress() < 1.0 && !img.isError()) {
                                logger.info("⏰ Timeout caricamento immagine cache disco per: " + cacheKey);
                                Platform.runLater(() -> {
                                    if (finalImageView.getImage() == null || finalImageView.getImage().getUrl().equals(PLACEHOLDER_IMAGE)) {
                                        // Se ancora non ha immagine valida, procedi con ricerca API
                                        searchWithParams(bookTitle, author, publishYear, finalImageView, finalOnImageLoaded, cacheKey, 1);
                                    }
                                });
                            }
                        } catch (InterruptedException ignored) {}
                    }).start();
                });

                return;
            } catch (Exception e) {
                logger.info("⚠️ Errore caricamento cache disco per: " + cacheKey + ", procedo con ricerca API");
                // Procedi con ricerca API invece di fermarti
                searchWithParams(bookTitle, author, publishYear, imageViewBase, onImageLoaded, cacheKey, 1);
                return;
            }
        }

        // 🔄 TERZA: Ricerca API e salva URL in cache disco
        // Dopo il precaricamento dello splash screen, permetti ancora tentativi API
        // ma con logica ottimizzata per evitare sprechi
        // Il precaricamento ha già provato le immagini più importanti

        // Prima tentativo: ricerca completa con tutti i parametri
        searchWithParams(bookTitle, author, publishYear, imageViewBase, onImageLoaded, cacheKey, 1);
    }

    /**
     * Cerca immagini con parametri specifici, con fallback progressivi
     * Dopo lo splash screen, opera in modalità silenziosa per non riempire i log
     */
    private static void searchWithParams(String bookTitle, String author, int publishYear,
                                        ImageView imageViewBase, Consumer<Image> onImageLoaded,
                                        String cacheKey, int attempt) {

        // Determina se siamo dopo lo splash screen (cache già popolata)
        boolean isPostSplash = imageCache.size() > 100; // Se abbiamo più di 100 immagini, siamo dopo splash
        boolean verbose = !isPostSplash || attempt == 1; // Solo primo tentativo o durante splash
        try {
            // HttpClient con timeout ridotto per gestire connessioni lente
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();

            // Pulizia e normalizzazione dei parametri
            String cleanTitle = cleanSearchTerm(bookTitle);
            String cleanAuthor = cleanSearchTerm(author);

            StringBuilder queryBuilder = new StringBuilder();

            // Strategia di ricerca basata sul tentativo
            switch (attempt) {
                case 1: // Tentativo completo
                    if (cleanTitle != null && !cleanTitle.isEmpty()) {
                        queryBuilder.append("title=").append(URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8)).append("&");
                    }
                    if (cleanAuthor != null && !cleanAuthor.isEmpty()) {
                        queryBuilder.append("author=").append(URLEncoder.encode(cleanAuthor, StandardCharsets.UTF_8)).append("&");
                    }
                    if (publishYear != 0) {
                        queryBuilder.append("first_publish_year=").append(publishYear).append("&");
                    }
                    break;

                case 2: // Solo titolo e autore (senza anno)
                    if (cleanTitle != null && !cleanTitle.isEmpty()) {
                        queryBuilder.append("title=").append(URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8)).append("&");
                    }
                    if (cleanAuthor != null && !cleanAuthor.isEmpty()) {
                        queryBuilder.append("author=").append(URLEncoder.encode(cleanAuthor, StandardCharsets.UTF_8)).append("&");
                    }
                    break;

                case 3: // Solo titolo (ricerca più permissiva)
                    if (cleanTitle != null && !cleanTitle.isEmpty()) {
                        queryBuilder.append("title=").append(URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8)).append("&");
                    }
                    break;

                case 4: // Ricerca fuzzy con parole chiave principali
                    String keywords = extractKeywords(cleanTitle);
                    if (keywords != null && !keywords.isEmpty()) {
                        queryBuilder.append("q=").append(URLEncoder.encode(keywords, StandardCharsets.UTF_8)).append("&");
                    } else if (cleanTitle != null && !cleanTitle.isEmpty()) {
                        queryBuilder.append("title=").append(URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8)).append("&");
                    }
                    break;
            }

            // Rimuovo l'ultimo & se presente
            if (queryBuilder.length() > 0 && queryBuilder.charAt(queryBuilder.length() - 1) == '&') {
                queryBuilder.deleteCharAt(queryBuilder.length() - 1);
            }

            if (queryBuilder.length() == 0) {
                // Nessun parametro valido, usa placeholder
                setPlaceholderImage(imageViewBase, onImageLoaded, cacheKey);
                return;
            }

            String url = "https://openlibrary.org/search.json?" + queryBuilder.toString();
            if (verbose) logger.info("Tentativo " + attempt + " - URL richiesta: " + url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(API_TIMEOUT)
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(response -> {
                        try {
                            JSONObject json = new JSONObject(response);
                            JSONArray docs = json.getJSONArray("docs");

                            JSONObject bestMatch = null;
                            int bestScore = 0;

                            for (int i = 0; i < Math.min(docs.length(), 20); i++) { // Limita a 20 risultati per performance
                                JSONObject book = docs.getJSONObject(i);
                                String foundTitle = book.optString("title", "").toLowerCase();

                                String foundAuthor = "";
                                if (book.has("author_name")) {
                                    JSONArray authorsArray = book.getJSONArray("author_name");
                                    if (authorsArray.length() > 0) {
                                        foundAuthor = authorsArray.getString(0).toLowerCase();
                                    }
                                }

                                int foundYear = book.optInt("first_publish_year", 0);

                                int score = calculateMatchScore(cleanTitle, cleanAuthor, publishYear,
                                                              foundTitle, foundAuthor, foundYear, attempt);

                                if (score > bestScore) {
                                    bestScore = score;
                                    bestMatch = book;
                                }
                            }

                            // Soglia di accettazione più bassa per tentativi successivi
                            int minScoreRequired = (attempt == 1) ? 3 : (attempt == 2) ? 2 : 1;

                             if (bestMatch != null && bestMatch.has("cover_i") && bestScore >= minScoreRequired) {
                                 int coverId = bestMatch.getInt("cover_i");
                                 String coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-L.jpg";
                                 if (verbose) logger.info("Copertina trovata (tentativo " + attempt + ", score: " + bestScore + "): " + coverUrl);

                                  // 💾 SALVA URL IN CACHE DISCO per utilizzi futuri
                                  ImageDiskCache.saveImageUrlToDisk(cacheKey, coverUrl);

                                   Image coverImage = new Image(coverUrl, true);
                                   imageCache.put(cacheKey, coverImage);
                                   logger.info("🌐 Immagine trovata via API e salvata in cache: " + cacheKey);

                                   // Attendi che l'immagine sia completamente caricata prima di chiamare il callback
                                   coverImage.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                                       if (newProgress.doubleValue() >= 1.0 && !coverImage.isError()) {
                                           logger.info("✅ Immagine completamente caricata per cache: " + cacheKey);
                                           Platform.runLater(() -> {
                                               imageViewBase.setImage(coverImage);
                                               if (onImageLoaded != null) {
                                                   onImageLoaded.accept(coverImage);
                                               }
                                           });
                                       } else if (coverImage.isError()) {
                                           logger.info("❌ Errore caricamento immagine API per: " + cacheKey);
                                           // In caso di errore, usa placeholder
                                           setPlaceholderImage(imageViewBase, onImageLoaded, cacheKey);
                                       }
                                   });

                                  // Aggiungi listener per gestire errori di caricamento immagine
                                  final ImageView finalImageViewBase = imageViewBase;
                                  final Consumer<Image> finalOnImageLoaded = onImageLoaded;
                                  coverImage.errorProperty().addListener((obs, oldError, newError) -> {
                                      if (newError) {
                                          logger.info("❌ Errore caricamento immagine dal URL: " + coverUrl);
                                          // In caso di errore, usa placeholder
                                          setPlaceholderImage(finalImageViewBase, finalOnImageLoaded, cacheKey);
                                      }
                                  });

                                  Platform.runLater(() -> {
                                      finalImageViewBase.setImage(coverImage);
                                      if (finalOnImageLoaded != null)
                                          finalOnImageLoaded.accept(coverImage);
                                  });
                            } else {
                                // Prova il tentativo successivo se disponibile
                                if (attempt < 4) {
                                    logger.info("Tentativo " + attempt + " fallito (score: " + bestScore + "), provo tentativo " + (attempt + 1));
                                    searchWithParams(bookTitle, author, publishYear, imageViewBase, onImageLoaded, cacheKey, attempt + 1);
                                } else {
                                    if (verbose) logger.info("Tutti i tentativi falliti, uso placeholder.");
                                    setPlaceholderImage(imageViewBase, onImageLoaded, cacheKey);
                                }
                            }

                        } catch (Exception e) {
                            if (verbose) e.printStackTrace();
                            // Prova tentativo successivo invece di fermarsi
                            if (attempt < 4) {
                                searchWithParams(bookTitle, author, publishYear, imageViewBase, onImageLoaded, cacheKey, attempt + 1);
                            } else {
                                setPlaceholderImage(imageViewBase, onImageLoaded, cacheKey);
                            }
                        }
                    })
                     .exceptionally(e -> {
                         // Gestisci errori di connessione internet in modo specifico
                         if (e.getCause() instanceof ConnectException || e.getMessage().contains("connect")) {
                             logger.info("🚫 Errore di connessione internet nel tentativo " + attempt + ": " + e.getMessage());
                             // In caso di errore di connessione, salta direttamente al placeholder
                             setPlaceholderImage(imageViewBase, onImageLoaded, cacheKey);
                             return null;
                         }

                         if (verbose) {
                             logger.info("⚠️ Errore HTTP nel tentativo " + attempt + ": " + e.getMessage());
                         }

                         // Prova tentativo successivo anche in caso di errore HTTP
                         if (attempt < 4) {
                             searchWithParams(bookTitle, author, publishYear, imageViewBase, onImageLoaded, cacheKey, attempt + 1);
                         } else {
                             logger.info("❌ Tutti i tentativi API falliti, uso placeholder.");
                             setPlaceholderImage(imageViewBase, onImageLoaded, cacheKey);
                         }
                         return null;
                     });

        } catch (Exception e) {
            logger.info("💥 Errore critico nel caricamento immagine per: " + cacheKey + " - " + e.getMessage());
            setPlaceholderImage(imageViewBase, onImageLoaded, cacheKey);
        }
    }

    /**
     * Calcola il punteggio di matching tra i parametri di ricerca e un libro trovato
     */
    private static int calculateMatchScore(String searchTitle, String searchAuthor, int searchYear,
                                         String foundTitle, String foundAuthor, int foundYear, int attempt) {
        int score = 0;

        // Matching del titolo (più importante)
        if (searchTitle != null && !searchTitle.isEmpty()) {
            String searchTitleLower = searchTitle.toLowerCase();
            String foundTitleLower = foundTitle.toLowerCase();

            if (foundTitleLower.equals(searchTitleLower)) {
                score += 10; // Match esatto
            } else if (foundTitleLower.contains(searchTitleLower)) {
                score += 5; // Contiene il titolo cercato
            } else if (searchTitleLower.contains(foundTitleLower)) {
                score += 3; // Il titolo cercato contiene quello trovato
            } else {
                // Controllo similarità fuzzy
                score += calculateFuzzyMatch(searchTitleLower, foundTitleLower);
            }
        }

        // Matching dell'autore
        if (searchAuthor != null && !searchAuthor.isEmpty() && !foundAuthor.isEmpty()) {
            String searchAuthorLower = searchAuthor.toLowerCase();
            if (foundAuthor.contains(searchAuthorLower)) {
                score += 4;
            } else if (searchAuthorLower.contains(foundAuthor)) {
                score += 2;
            }
        }

        // Matching dell'anno (meno importante per tentativi successivi)
        if (searchYear != 0 && foundYear != 0) {
            int yearDiff = Math.abs(searchYear - foundYear);
            if (yearDiff == 0) {
                score += (attempt == 1) ? 3 : 1; // Anno esatto più importante nel primo tentativo
            } else if (yearDiff <= 2) {
                score += 1; // Anno vicino
            }
        }

        // Bonus per tentativi successivi (più permissivi)
        if (attempt > 1) {
            score += (attempt - 1);
        }

        return score;
    }

    /**
     * Calcola similarità fuzzy tra due stringhe
     */
    private static int calculateFuzzyMatch(String str1, String str2) {
        if (str1 == null || str2 == null || str1.isEmpty() || str2.isEmpty()) {
            return 0;
        }

        // Controllo se condividono parole significative
        String[] words1 = str1.split("\\s+");
        String[] words2 = str2.split("\\s+");

        int commonWords = 0;
        for (String word1 : words1) {
            if (word1.length() > 2) { // Solo parole significative
                for (String word2 : words2) {
                    if (word2.length() > 2 && word1.equals(word2)) {
                        commonWords++;
                        break;
                    }
                }
            }
        }

        return Math.min(commonWords * 2, 4); // Max 4 punti per parole comuni
    }

    /**
     * Pulisce un termine di ricerca rimuovendo caratteri speciali e normalizzando
     */
    private static String cleanSearchTerm(String term) {
        if (term == null || term.trim().isEmpty()) {
            return null;
        }

        // Rimuovi caratteri speciali e normalizza spazi
        String cleaned = term.replaceAll("[^a-zA-Z0-9\\s]", " ")
                            .replaceAll("\\s+", " ")
                            .trim();

        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Estrae parole chiave principali da un titolo per ricerca fuzzy
     */
    private static String extractKeywords(String title) {
        if (title == null || title.trim().isEmpty()) {
            return null;
        }

        String[] words = title.toLowerCase().split("\\s+");
        StringBuilder keywords = new StringBuilder();

        // Prendi le prime 3 parole significative (lunghezza > 2)
        int count = 0;
        for (String word : words) {
            if (word.length() > 2 && count < 3) {
                if (keywords.length() > 0) keywords.append(" ");
                keywords.append(word);
                count++;
            }
        }

        return keywords.toString().trim();
    }

    /**
     * Imposta l'immagine placeholder
     */
    private static void setPlaceholderImage(ImageView imageView, Consumer<Image> onImageLoaded, String cacheKey) {
        try {
            Image placeholder = new Image(PLACEHOLDER_IMAGE, false); // Synchronous loading per semplicità
            imageCache.put(cacheKey, placeholder);
            Platform.runLater(() -> {
                imageView.setImage(placeholder);
                if (onImageLoaded != null)
                    onImageLoaded.accept(placeholder);
            });
        } catch (Exception e) {
            logger.info("🚨 ERRORE caricamento placeholder per: " + cacheKey + " - " + e.getMessage());
            // Fallback: immagine vuota
            try {
                Image emptyImage = new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==", 1, 1, false, false);
                imageCache.put(cacheKey, emptyImage);
                Platform.runLater(() -> {
                    imageView.setImage(emptyImage);
                    if (onImageLoaded != null) onImageLoaded.accept(emptyImage);
                });
            } catch (Exception e2) {
                logger.info("🚨 ERRORE COMPLETAMENTE IRRECUPERABILE per: " + cacheKey);
                Platform.runLater(() -> {
                    imageView.setImage(null);
                    if (onImageLoaded != null) onImageLoaded.accept(null);
                });
            }
        }
    }

    public static Image getCachedOrPlaceholder(Book book) {
        String key = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();
        Image cached = imageCache.get(key);
        if (cached != null) return cached;
        Image placeholder = new Image(PLACEHOLDER_IMAGE);
        imageCache.put(key, placeholder);
        return placeholder;
    }

    /**
     * Forza il posizionamento del placeholder per un libro specifico
     * Usato dalla splash screen quando il timeout per il caricamento immagini scade
     */
    public static void forcePlaceholderForBook(Book book) {
        String cacheKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();

        // Solo se non è già in cache
        if (!imageCache.containsKey(cacheKey)) {
            try {
                Image placeholder = new Image(PLACEHOLDER_IMAGE, false); // Caricamento sincrono per immediatezza
                imageCache.put(cacheKey, placeholder);
                logger.info("🔧 Placeholder forzato nella cache per: " + book.getTitle());
            } catch (Exception e) {
                System.err.println("🚨 Errore forzatura placeholder per: " + book.getTitle() + " - " + e.getMessage());
                // Fallback con immagine vuota
                try {
                    Image emptyImage = new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==", 1, 1, false, false);
                    imageCache.put(cacheKey, emptyImage);
                } catch (Exception e2) {
                    System.err.println("🚨 Errore critico forzatura placeholder per: " + book.getTitle());
                }
            }
        }
    }

    /**
     * Imposta direttamente l'immagine dalla cache se disponibile, altrimenti carica dal server
     * Ottimizzato per ridurre chiamate duplicate dopo il precaricamento dello splash screen
     */
    public static void setImageFromCacheOrLoad(Book book, ImageView imageView, Consumer<Image> onImageLoaded) {
        String cacheKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();

        // Prima controlla se l'immagine è già in cache (caricata durante splash screen)
        if (imageCache.containsKey(cacheKey)) {
            Image cachedImage = imageCache.get(cacheKey);
            logger.info("✅ Cache hit per homepage: " + book.getTitle() + " - Cache key: " + cacheKey);
            // Imposta direttamente senza callback aggiuntivi per performance
            Platform.runLater(() -> {
                imageView.setImage(cachedImage);
                if (onImageLoaded != null) {
                    onImageLoaded.accept(cachedImage);
                }
            });
            return;
        }

        // Se non è in cache, cerca online (anche dopo il precaricamento)
        // Questo permette di scaricare immagini per libri non precaricati

        // Se non è in cache E siamo ancora in fase di caricamento iniziale, carica normalmente
        logger.info("🔍 Ricerca immagine per homepage: " + book.getTitle() + " - Cache key: " + cacheKey + " - Cache size: " + imageCache.size());
        findCoverImage(book.getTitle(), book.getAuthors(), book.getPublish_date_year(), imageView, onImageLoaded);
    }

    /**
     * Verifica se un'immagine è già presente in cache
     */
    public static boolean isImageInCache(String cacheKey) {
        return imageCache.containsKey(cacheKey);
    }

    /**
     * Carica immagine direttamente in cache senza ImageView (ottimizzato per splash screen)
     * Ritorna un CompletableFuture che si completa quando l'immagine è stata caricata E SALVATA IN CACHE
     */
    public static CompletableFuture<Image> preloadImageToCache(Book book) {
        String cacheKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();

        // Se già in cache, completa immediatamente
        if (imageCache.containsKey(cacheKey)) {
            return CompletableFuture.completedFuture(imageCache.get(cacheKey));
        }

        CompletableFuture<Image> future = new CompletableFuture<>();

        // Carica l'immagine usando un ImageView temporaneo invisibile
        ImageView tempImageView = new ImageView();
        tempImageView.setVisible(false);

        findCoverImage(book.getTitle(), book.getAuthors(), book.getPublish_date_year(), tempImageView,
            loadedImage -> {
                // ATTENDI che l'immagine sia completamente caricata prima di completare il futuro
                if (loadedImage != null && loadedImage.getProgress() >= 1.0 && !loadedImage.isError()) {
                    // 🐛 DEBUG: Verifica che l'immagine sia stata messa in cache
                    String debugCacheKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();
                    boolean inCache = imageCache.containsKey(debugCacheKey);
                    logger.info("🔍 Preload completato per: " + book.getTitle() +
                        " - In cache memoria: " + inCache + " - Progress: " + loadedImage.getProgress());

                    future.complete(loadedImage);
                } else if (loadedImage != null) {
                    // Se l'immagine non è ancora caricata completamente, attendi
                    loadedImage.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                        if (newProgress.doubleValue() >= 1.0 && !loadedImage.isError()) {
                            String debugCacheKey = book.getTitle() + "_" + book.getAuthors() + "_" + book.getPublish_date_year();
                            boolean inCache = imageCache.containsKey(debugCacheKey);
                            logger.info("🔍 Preload completato post-attesa per: " + book.getTitle() +
                                " - In cache memoria: " + inCache + " - Progress: " + newProgress.doubleValue());

                            future.complete(loadedImage);
                        } else if (loadedImage.isError()) {
                            // Se c'è un errore, completa comunque per evitare blocchi
                            logger.info("⚠️ Errore caricamento immagine durante preload per: " + book.getTitle());
                            future.complete(loadedImage);
                        }
                    });
                } else {
                    // Immagine null o placeholder
                    future.complete(loadedImage);
                }
            }
        );

        return future;
    }
}
