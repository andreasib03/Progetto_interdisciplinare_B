package it.uninsubria.client.utils.classesUI;

import it.uninsubria.shared.model.Book;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionManagerTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testRecentlyOpenedBooks() {
        // Test della funzionalità libri recentemente aperti
        SessionManager sessionManager = SessionManager.getInstance();

        // Simula un utente loggato per il test
        SessionManager.UserSession testSession = new SessionManager.UserSession("testUser", "testuser", "test@example.com", "testToken");
        // Aggiungi la sessione alla mappa interna (accesso reflection per test)
        try {
            java.lang.reflect.Field field = SessionManager.class.getDeclaredField("userSessions");
            field.setAccessible(true);
            java.util.Map<String, SessionManager.UserSession> userSessions =
                (java.util.Map<String, SessionManager.UserSession>) field.get(sessionManager);
            userSessions.put("testUser", testSession);

            // Imposta l'utente attivo
            java.lang.reflect.Field activeField = SessionManager.class.getDeclaredField("activeUserId");
            activeField.setAccessible(true);
            activeField.set(sessionManager, "testUser");

        } catch (Exception e) {
            fail("Impossibile configurare il test: " + e.getMessage());
        }

        // Crea alcuni libri di test
        Book book1 = new Book();
        book1.setBook_id(1);
        book1.setTitle("Libro 1");
        book1.setAuthors("Autore 1");

        Book book2 = new Book();
        book2.setBook_id(2);
        book2.setTitle("Libro 2");
        book2.setAuthors("Autore 2");

        Book book3 = new Book();
        book3.setBook_id(3);
        book3.setTitle("Libro 3");
        book3.setAuthors("Autore 3");

        // Aggiungi libri alla lista dei recentemente aperti
        sessionManager.addRecentlyOpenedBook(book1);
        sessionManager.addRecentlyOpenedBook(book2);
        sessionManager.addRecentlyOpenedBook(book1); // Ri-aggiungi book1 (dovrebbe spostarsi in cima)
        sessionManager.addRecentlyOpenedBook(book3);

        // Verifica che la lista sia corretta
        var recentlyOpened = sessionManager.getRecentlyOpenedBooks();

        // Dovrebbero essere 3 libri (book3, book1, book2)
        assertEquals(3, recentlyOpened.size());
        assertEquals("Libro 3", recentlyOpened.get(0).getTitle()); // Ultimo aggiunto
        assertEquals("Libro 1", recentlyOpened.get(1).getTitle()); // Spostato in cima quando ri-aggiunto
        assertEquals("Libro 2", recentlyOpened.get(2).getTitle()); // Primo aggiunto

        System.out.println("✅ Test funzionalità libri recentemente aperti superato!");
    }
}