package it.uninsubria.server.util;


import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import it.uninsubria.server.cache.CacheManager;
import it.uninsubria.shared.exception.ValidationException;
import it.uninsubria.shared.model.User;

public class InputValidatorEdgeCaseTest {

    @Before
    public void setUp() {
        CacheManager.clear();
    }

    @After
    public void tearDown() {
        CacheManager.clear();
    }

    @Test(expected = ValidationException.class)
    public void testNullUsername() throws ValidationException {
        InputValidator.validateUsername(null);
    }

    @Test(expected = ValidationException.class)
    public void testEmptyUsername() throws ValidationException {
        InputValidator.validateUsername("");
    }

    @Test(expected = ValidationException.class)
    public void testTooLongUsername() throws ValidationException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 51; i++) {
            sb.append("a");
        }
        InputValidator.validateUsername(sb.toString());
    }

    @Test(expected = ValidationException.class)
    public void testUsernameWithSpaces() throws ValidationException {
        InputValidator.validateUsername("user name");
    }

    @Test
    public void testValidUsername() throws ValidationException {
        InputValidator.validateUsername("validuser123");
        InputValidator.validateUsername("username");
    }

    @Test(expected = ValidationException.class)
    public void testNullEmail() throws ValidationException {
        InputValidator.validateEmail(null);
    }

    @Test(expected = ValidationException.class)
    public void testEmptyEmail() throws ValidationException {
        InputValidator.validateEmail("");
    }

    @Test(expected = ValidationException.class)
    public void testTooLongEmail() throws ValidationException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 321; i++) {
            sb.append("a");
        }
        InputValidator.validateEmail(sb.toString() + "@test.com");
    }

    @Test(expected = ValidationException.class)
    public void testInvalidEmailFormat() throws ValidationException {
        InputValidator.validateEmail("invalid-email");
        InputValidator.validateEmail("invalid@");
        InputValidator.validateEmail("@test.com");
    }

    @Test
    public void testValidEmail() throws ValidationException {
        InputValidator.validateEmail("user@example.com");
        InputValidator.validateEmail("user.name@example.co.uk");
        InputValidator.validateEmail("user+tag@example.org");
    }

    @Test(expected = ValidationException.class)
    public void testNullPassword() throws ValidationException {
        InputValidator.validatePassword(null);
    }

    @Test(expected = ValidationException.class)
    public void testTooShortPassword() throws ValidationException {
        InputValidator.validatePassword("abc");
    }

    @Test(expected = ValidationException.class)
    public void testTooLongPassword() throws ValidationException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 101; i++) {
            sb.append("a");
        }
        InputValidator.validatePassword(sb.toString());
    }

    @Test
    public void testValidPassword() throws ValidationException {
        InputValidator.validatePassword("ValidPassword123!");
    }

    @Test(expected = ValidationException.class)
    public void testNegativeYear() throws ValidationException {
        InputValidator.validateYear(-1);
    }

    @Test(expected = ValidationException.class)
    public void testTooEarlyYear() throws ValidationException {
        InputValidator.validateYear(999);
    }

    @Test(expected = ValidationException.class)
    public void testTooLateYear() throws ValidationException {
        int futureYear = java.time.Year.now().getValue() + 11;
        InputValidator.validateYear(futureYear);
    }

    @Test
    public void testValidYear() throws ValidationException {
        int currentYear = java.time.Year.now().getValue();
        InputValidator.validateYear(2023);
        InputValidator.validateYear(currentYear);
    }

    @Test(expected = ValidationException.class)
    public void testNullCodiceFiscale() throws ValidationException {
        InputValidator.validateCodiceFiscale(null);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidCodiceFiscaleLength() throws ValidationException {
        InputValidator.validateCodiceFiscale("RSSMRA");
    }

    @Test(expected = ValidationException.class)
    public void testInvalidCodiceFiscaleCharacters() throws ValidationException {
        InputValidator.validateCodiceFiscale("RSSMRA00A01H50112345678901");
    }

    @Test
    public void testValidCodiceFiscale() throws ValidationException {
        InputValidator.validateCodiceFiscale("RSSMRA80A01H501O");
        InputValidator.validateCodiceFiscale("QWERTY32G54T654Y");
    }

    @Test(expected = ValidationException.class)
    public void testNullSearchQuery() throws ValidationException {
        InputValidator.validateSearchQuery(null);
    }

    @Test(expected = ValidationException.class)
    public void testEmptySearchQuery() throws ValidationException {
        InputValidator.validateSearchQuery("");
    }

    @Test(expected = ValidationException.class)
    public void testTooLongSearchQuery() throws ValidationException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10001; i++) {
            sb.append("a");
        }
        InputValidator.validateSearchQuery(sb.toString());
    }

    @Test
    public void testValidSearchQuery() throws ValidationException {
        InputValidator.validateSearchQuery("test query");
        InputValidator.validateSearchQuery("Harry Potter");
        InputValidator.validateSearchQuery("Book Title");
    }

    @Test(expected = ValidationException.class)
    public void testNullUser() throws ValidationException {
        InputValidator.validateUserRegistration(null);
    }

    @Test(expected = ValidationException.class)
    public void testInvalidUserEmptyFields() throws ValidationException {
        User u = User.builder()
            .id("test")
            .name("")
            .surname("Smith")
            .email("test@example.com")
            .build();
        InputValidator.validateUserRegistration(u);
    }

    @Test
    public void testValidUser() throws ValidationException {
        User u = User.builder()
            .id("testuser")
            .name("John")
            .surname("Doe")
            .email("john@example.com")
            .CF("RSSMRA80A01H501O")
            .password("ValidPassword123!")
            .build();
        InputValidator.validateUserRegistration(u);
    }
}
