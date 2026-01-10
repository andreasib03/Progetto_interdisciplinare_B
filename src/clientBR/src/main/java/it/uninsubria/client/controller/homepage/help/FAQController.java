package it.uninsubria.client.controller.homepage.help;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;

public class FAQController {

    @FXML
    private Accordion accordion1;
    @FXML
    private Accordion accordion2;
    @FXML
    private Accordion accordion3;
    @FXML
    private Accordion accordion4;

    @FXML
    private TitledPane titledpane1;
    @FXML
    private TitledPane titledpane2;
    @FXML
    private TitledPane titledpane3;
    @FXML
    private TitledPane titledpane4;

    @FXML
    private void initialize() {
        for(Accordion accordion : new Accordion[]{accordion1, accordion2, accordion3, accordion4}) {
            setupAccordion(accordion);
        }
    }

    private void setupAccordion(Accordion accordion) {
        accordion.setExpandedPane(null); // all’avvio parte chiuso
        accordion.expandedPaneProperty().addListener((obs, oldPane, newPane) -> {
            for(TitledPane pane : accordion.getPanes()) {
                if (pane == newPane) {
                    Node content = pane.lookup(".title"); // cerca il nodo interno con la classe CSS
                }
            
                if (oldPane == newPane) {
                accordion.setExpandedPane(null); // permette richiudere con click
                }
            }
        });
    }
}
