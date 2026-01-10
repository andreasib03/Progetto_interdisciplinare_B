package it.uninsubria.shared.model;
import javafx.beans.property.SimpleStringProperty;

public class ReviewsRecord {

    
    private final SimpleStringProperty utente;
    private final SimpleStringProperty libro;
    private final SimpleStringProperty libreria;
    private final SimpleStringProperty stile;
    private final SimpleStringProperty contenuto;
    private final SimpleStringProperty gradevolezza;
    private final SimpleStringProperty originalità;
    private final SimpleStringProperty edizione;
    private final SimpleStringProperty votoFinale;
    private final SimpleStringProperty note;
    private final SimpleStringProperty mediaPunteggio;

    /**
     * Costruttore per inizializzare un nuovo record di valutazione.
     * 
     * <p>Ogni parametro del costruttore rappresenta una specifica informazione associata alla valutazione di un libro.</p>
     *
     * @param utente Il nome dell'utente che ha effettuato la valutazione.
     * @param libro Il titolo del libro oggetto della valutazione.
     * @param libreria Il nome della libreria associata alla valutazione.
     * @param stile La valutazione sullo stile del libro.
     * @param contenuto La valutazione sul contenuto del libro.
     * @param gradevolezza La valutazione sulla gradevolezza del libro.
     * @param originalità La valutazione sull'originalità del libro.
     * @param edizione La valutazione sull'edizione del libro.
     * @param votoFinale Il voto finale assegnato al libro.
     * @param note Eventuali note aggiuntive fornite dall'utente.
     * @param mediaPunteggio La media dei punteggi ottenuti dal libro.
     */
    public ReviewsRecord(String utente, String libro, String libreria, String stile, String contenuto,
                             String gradevolezza, String originalità, String edizione, String votoFinale,
                             String note, String mediaPunteggio) {
        this.utente = new SimpleStringProperty(utente);
        this.libro = new SimpleStringProperty(libro);
        this.libreria = new SimpleStringProperty(libreria);
        this.stile = new SimpleStringProperty(stile);
        this.contenuto = new SimpleStringProperty(contenuto);
        this.gradevolezza = new SimpleStringProperty(gradevolezza);
        this.originalità = new SimpleStringProperty(originalità);
        this.edizione = new SimpleStringProperty(edizione);
        this.votoFinale = new SimpleStringProperty(votoFinale);
        this.note = new SimpleStringProperty(note);
        this.mediaPunteggio = new SimpleStringProperty(mediaPunteggio);
    }

    // Getters per le proprietà

    /**
     * Restituisce il nome dell'utente che ha effettuato la valutazione.
     * 
     * @return Il nome dell'utente che ha effettuato la valutazione.
     */
    public String getUtente(){
        return utente.get();
    }

    /**
     * Restituisce il titolo del libro oggetto della valutazione.
     * 
     * @return Il titolo del libro.
     */
    public String getLibro(){
        return libro.get();
    }

    /**
     * Restituisce il nome della libreria associata alla valutazione.
     * 
     * @return Il nome della libreria.
     */
    public String getLibreria(){
        return libreria.get();
    }

    /**
     * Restituisce la valutazione sullo stile del libro.
     * 
     * @return La valutazione sullo stile.
     */
    public String getStile(){
        return stile.get();
    }

    /**
     * Restituisce la valutazione sul contenuto del libro.
     * 
     * @return La valutazione sul contenuto.
     */
    public String getContenuto(){
        return contenuto.get();
    }

    /**
     * Restituisce la valutazione sulla gradevolezza del libro.
     * 
     * @return La valutazione sulla gradevolezza.
     */
    public String getGradevolezza(){
        return gradevolezza.get();
    }

    /**
     * Restituisce la valutazione sull'originalità del libro.
     * 
     * @return La valutazione sull'originalità.
     */
    public String getOriginalità(){
        return originalità.get();
    }

    /**
     * Restituisce la valutazione sull'edizione del libro.
     * 
     * @return La valutazione sull'edizione.
     */
    public String getEdizione(){
        return edizione.get();
    }

    /**
     * Restituisce il voto finale assegnato al libro.
     * 
     * @return Il voto finale assegnato al libro.
     */
    public String getVotoFinale(){
        return votoFinale.get();
    }

    /**
     * Restituisce eventuali note aggiuntive fornite dall'utente.
     * 
     * @return Le note aggiuntive fornite dall'utente.
     */
    public String getNote(){
        return note.get();
    }

    /**
     * Restituisce la media dei punteggi ottenuti dal libro.
     * 
     * @return La media dei punteggi ottenuti dal libro.
     */
    public String getMediaPunteggio(){
        return mediaPunteggio.get();
    }
}


