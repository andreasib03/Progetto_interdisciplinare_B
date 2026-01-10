package it.uninsubria.client.controller.homepage.help;

import it.uninsubria.client.controller.ControllerBase;
import java.awt.Desktop;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import javafx.scene.input.KeyCode;

import it.uninsubria.client.utils.classesUI.AlertMessageCustom;
import it.uninsubria.client.utils.classesUI.EveryView;
import it.uninsubria.client.utils.classesUI.Navigator;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class viewAbouts extends ControllerBase {

    

    @FXML
    private VBox vboxDocsUtente;
    @FXML
    private VBox vboxDocsTecnico;
    @FXML
    private VBox vboxFAQ;
    @FXML 
    private VBox vboxVideo;
    @FXML
    private VBox vboxFeedback;

    @FXML
    private TextField searchingBarAbout;
    
    @FXML
    private HBox HBoxContent;

    @FXML
    private Button buttonSearching;

    private final ContextMenu suggestionsPopup = new ContextMenu();
    private final Map<VBox, Runnable> vboxActions = new HashMap<>();

    // Parole chiave per ogni sezione per migliorare la ricerca
    private final Map<VBox, List<String>> vboxKeywords = new HashMap<>();

    @FXML
    private void initialize(){

        searchingBarAbout.setFocusTraversable(true);
        searchingBarAbout.requestFocus();
        buttonSearching.setFocusTraversable(false);

        // ✅ Implementate tutte e 3 le funzionalità di ricerca avanzata
        
        settingCursor();
        initializeSearchActions();
        setupSearchBarListener();
    }

    

    private void initializeSearchActions(){
        vboxActions.put(vboxDocsUtente, () -> salvaEApriPdf(EveryView.PDF_MANUAL_USER.getPath(), "Manuale_utente"));
        vboxActions.put(vboxDocsTecnico, () -> salvaEApriPdf(EveryView.PDF_MANUAL_TECNICAL.getPath(), "Manuale_tecnico"));
        vboxActions.put(vboxFeedback, () -> Navigator.openNewWindow(EveryView.FORM_CONTACT.getPath(), resolveString("%help.feedback")));
        vboxActions.put(vboxVideo, () -> Navigator.openNewWindow(EveryView.VIDEO_VIEW.getPath(), resolveString("%help.video")));
        vboxActions.put(vboxFAQ, () -> Navigator.openNewWindow(EveryView.FAQ_VIEW.getPath(), resolveString("%help.faq")));

        // Inizializza parole chiave per ricerca avanzata
        vboxKeywords.put(vboxDocsUtente, Arrays.asList("manuale", "utente", "user", "guida", "istruzioni", "come usare", "tutorial"));
        vboxKeywords.put(vboxDocsTecnico, Arrays.asList("tecnico", "technical", "sviluppatore", "developer", "api", "documentazione", "specifiche"));
        vboxKeywords.put(vboxFeedback, Arrays.asList("feedback", "contatto", "contact", "segnalazione", "bug", "problema", "suggerimento"));
        vboxKeywords.put(vboxVideo, Arrays.asList("video", "tutorial", "istruzioni", "visuale", "demo", "esempi"));
        vboxKeywords.put(vboxFAQ, Arrays.asList("faq", "domande", "risposte", "frequenti", "aiuto", "problemi comuni"));
    }

    private void setupSearchBarListener() {
        searchingBarAbout.textProperty().addListener((obs, oldText, newText) -> {
            suggestionsPopup.getItems().clear();
            if (!newText.isBlank()) {
                String searchTerm = newText.toLowerCase().trim();
                vboxActions.keySet().forEach(vbox -> {
                    String label = getLabelForVBox(vbox);
                    // Ricerca avanzata: controlla se il termine di ricerca corrisponde
                    // al label, alle parole chiave, o è contenuto nel label
                    if (matchesSearch(label, searchTerm, vbox)) {
                        MenuItem item = new MenuItem(label);
                        item.setOnAction(evt -> handleVBoxAction(vbox));
                        suggestionsPopup.getItems().add(item);
                    }
                });

                if (!suggestionsPopup.getItems().isEmpty() && !suggestionsPopup.isShowing()) {
                    suggestionsPopup.show(searchingBarAbout, javafx.geometry.Side.BOTTOM, 0, 0);
                }
            } else {
                suggestionsPopup.hide();
            }
        });

        // Implementazione: se premo invio si apra il primo risultato
        searchingBarAbout.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (!suggestionsPopup.getItems().isEmpty()) {
                    // Apri il primo risultato
                    MenuItem firstItem = suggestionsPopup.getItems().get(0);
                    firstItem.fire();
                    suggestionsPopup.hide();
                }
                event.consume();
            }
        });

        // Implementazione: se clicco fuori dalla barra di ricerca, si chiuda il menu a tendina
        searchingBarAbout.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnMouseClicked(event -> {
                    // Controlla se il click è fuori dalla barra di ricerca
                    if (!searchingBarAbout.getBoundsInParent().contains(event.getX(), event.getY())) {
                        suggestionsPopup.hide();
                    }
                });
            }
        });
    }

    private String getLabelForVBox(VBox vbox) {
        if (vbox == vboxDocsUtente) return resolveString("%help.userdoc");
        if (vbox == vboxDocsTecnico) return resolveString("%help.techdoc");
        if (vbox == vboxFAQ) return resolveString("%help.faq");
        if (vbox == vboxVideo) return resolveString("%help.video");
        if (vbox == vboxFeedback) return resolveString("%help.feedback");
        return "";
    }

    /**
     * Implementazione ricerca avanzata: cerca parole chiave in tutte le vbox
     * e non solo all'inizio della stringa
     */
    private boolean matchesSearch(String label, String searchTerm, VBox vbox) {
        String labelLower = label.toLowerCase();

        // 1. Controlla se il termine è contenuto nel label (ricerca ovunque)
        if (labelLower.contains(searchTerm)) {
            return true;
        }

        // 2. Controlla se il termine corrisponde a parole chiave specifiche
        List<String> keywords = vboxKeywords.get(vbox);
        if (keywords != null) {
            for (String keyword : keywords) {
                if (keyword.toLowerCase().contains(searchTerm) ||
                    searchTerm.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        // 3. Controlla corrispondenze parziali più flessibili
        String[] searchWords = searchTerm.split("\\s+");
        for (String word : searchWords) {
            if (word.length() > 2) { // Solo parole significative
                if (labelLower.contains(word)) {
                    return true;
                }
                // Controlla anche nelle keywords
                if (keywords != null) {
                    for (String keyword : keywords) {
                        if (keyword.toLowerCase().contains(word)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }


    private void salvaEApriPdf(String percorsoRisorsa, String nomeFile) {
        try (InputStream input = getClass().getResourceAsStream(percorsoRisorsa)) {
            if (input == null) {
                AlertMessageCustom.showInfo(null, resolveString("%help.pdf.window"), resolveString("%help.pdf.not.found"));
                return;
            }

            File tempFile = File.createTempFile(nomeFile, ".pdf");
            tempFile.deleteOnExit();

            try (FileOutputStream output = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            Desktop.getDesktop().open(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void settingCursor() {
        for (VBox vbox : new VBox[]{vboxDocsUtente, vboxDocsTecnico, vboxFAQ, vboxVideo, vboxFeedback}) {
            vbox.setOnMouseEntered(e -> vbox.setStyle(vbox.getStyle() + "-fx-cursor: hand;"));
            vbox.setOnMouseExited(e -> vbox.setStyle(vbox.getStyle() + "-fx-cursor: default;"));
        }
    }


    private void startAnimationSelected(VBox selectedVBox) {
        for (VBox vbox : new VBox[]{vboxDocsUtente, vboxDocsTecnico, vboxFAQ, vboxVideo, vboxFeedback}) {
            if (vbox == selectedVBox) {
                vbox.setStyle("-fx-background-color: #E0F0FF; -fx-border-color: #3D8DF5; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");
            } else {
                vbox.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
            }
        }

    }

    private void resetAnimation() {
        for (VBox vbox : new VBox[]{vboxDocsUtente, vboxDocsTecnico, vboxFAQ, vboxVideo, vboxFeedback}) {
            vbox.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        }
    }

    private void handleVBoxAction(VBox vbox) {
        Runnable action = vboxActions.get(vbox);
        if (action == null){
            System.err.println("Nessuna azione associata a questa VBox.");
            return;
        }
        startAnimationSelected(vbox);
        try{
            action.run();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        PauseTransition pause = new PauseTransition(Duration.millis(400)); // 400ms di evidenziazione
        pause.setOnFinished(e -> resetAnimation());
        pause.play();
    }


    @FXML
    private void downloadManualDocs(ActionEvent event) {
        handleVBoxAction(vboxDocsUtente);
    }

    @FXML
    private void downloadTecnicalDocs(ActionEvent event) {
        handleVBoxAction(vboxDocsTecnico);
    }

    @FXML
    private void onOpenContactForm() {
        handleVBoxAction(vboxFeedback);
    }

    @FXML
    private void onOpenVideoList() {
        handleVBoxAction(vboxVideo);
    }

    @FXML
    private void onOpenFAQ() {
        handleVBoxAction(vboxFAQ);
    }

}
