# Librerie Esterne - Book Recommender System

Questo progetto utilizza Maven per la gestione delle dipendenze. Le librerie esterne vengono automaticamente scaricate e incluse nel classpath durante la compilazione.

## Dipendenze Principali

### JavaFX (Client)
- **javafx-controls**: ${javafx.version} (19.0.2.1)
- **javafx-fxml**: ${javafx.version} (19.0.2.1)

### UI e Icone
- **ikonli-javafx**: 12.3.1 - Framework per icone JavaFX
- **ikonli-fontawesome5-pack**: 12.3.1 - Pack icone FontAwesome 5
- **ikonli-materialdesign-pack**: 12.3.1 - Pack icone Material Design

### Sicurezza e Autenticazione
- **jjwt-impl**: 0.12.5 - Implementazione JWT
- **jjwt-jackson**: 0.12.5 - Supporto Jackson per JWT

### Logging
- **slf4j-api**: 1.7.36 - Simple Logging Facade for Java
- **logback-classic**: 1.2.11 - Implementazione Logback

### Database
- **postgresql**: 42.5.0 - Driver PostgreSQL
- **h2**: 2.1.214 (solo test) - Database H2 in-memory

### JSON Processing
- **jackson-databind**: 2.15.2 - Elaborazione JSON
- **jackson-datatype-jsr310**: 2.15.2 - Supporto date/time Java 8

### Monitoring e Resilience
- **micrometer-core**: 1.12.0 - Framework metriche
- **micrometer-registry-jmx**: 1.12.0 - Registry JMX per Micrometer
- **resilience4j-circuitbreaker**: 2.2.0 - Pattern Circuit Breaker
- **resilience4j-micrometer**: 2.2.0 - Integrazione Micrometer

### Testing
- **junit**: 4.13.2 - Framework di testing

### Build Tools
- **lombok**: 1.18.32 - Generazione codice automatica
- **maven-compiler-plugin**: 3.13.0 - Plugin compilazione Maven
- **javafx-maven-plugin**: 0.0.8 - Plugin JavaFX Maven
- **maven-checkstyle-plugin**: 3.3.0 - Plugin controllo stile codice

## Installazione Dipendenze

Per scaricare automaticamente tutte le dipendenze:

```bash
mvn clean install
```

Questo comando scaricherà tutte le librerie necessarie dal Maven Central Repository e le posizionerà nella cartella locale `~/.m2/repository`.

## Dipendenze Runtime

Le seguenti dipendenze sono necessarie per l'esecuzione dell'applicazione:

### Server
- PostgreSQL JDBC Driver
- JWT libraries
- Logging framework (SLF4J + Logback)
- Micrometer per monitoring
- Resilience4j per fault tolerance

### Client
- JavaFX runtime
- Ikonli per icone
- Jackson per JSON
- PostgreSQL JDBC (per alcune operazioni client)

## Ambiente di Sviluppo

Assicurati di avere installato:
- JDK 17+
- Maven 3.6+
- PostgreSQL 12+

## Note sulle Dipendenze

- **Lombok**: Richiede configurazione IDE per il supporto alle annotazioni
- **JavaFX**: Non incluso nel JDK standard, richiede download separato per runtime
- **PostgreSQL**: Database esterno necessario per il funzionamento completo
- **H2**: Utilizzato solo per i test, non necessario per produzione

## Sicurezza delle Dipendenze

Tutte le dipendenze vengono verificate per vulnerabilità note attraverso:
- Maven Dependency Check Plugin
- Snyk Security Scanner (consigliato)
- OWASP Dependency Check

Per verificare vulnerabilità:
```bash
mvn org.owasp:dependency-check-maven:check
```</content>
<parameter name="filePath">/home/projectb/Documenti/ProjectB/lib/README.md