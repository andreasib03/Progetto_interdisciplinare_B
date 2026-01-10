#!/bin/bash

# ============================================================================
# Script Migrazione Password - Book Recommender System
# ============================================================================
#
# Questo script migra le password in plaintext dal database a BCrypt
# Utilizza l'API Java dell'applicazione per l'hashing
# ============================================================================

set -e  # Exit on error

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ============================================================================
# CONFIGURAZIONE
# ============================================================================

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-bookrecommender}"
DB_USER="${DB_USER:-postgres}"

# ============================================================================
# FUNZIONI DI UTILITY
# ============================================================================

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_step() {
    echo -e "${GREEN}[STEP]${NC} $1"
}

# Verifica prerequisiti
check_prerequisites() {
    print_step "Verifica prerequisiti..."

    # Verifica psql è installato
    if ! command -v psql &> /dev/null; then
        print_error "psql non è installato. Installalo con:"
        echo "  Ubuntu/Debian: sudo apt-get install postgresql-client"
        echo "  macOS: brew install postgresql"
        exit 1
    fi

    # Verifica Java è installato
    if ! command -v java &> /dev/null; then
        print_error "Java non è installato. Installa JDK 17 o superiore."
        exit 1
    fi

    print_info "Tutti i prerequisiti sono soddisfatti"
}

# Chiede conferma prima di procedere
confirm_migration() {
    echo ""
    echo -e "${YELLOW}⚠️  ATTENZIONE: Questo script modificherà il database!${NC}"
    echo ""
    read -p "Continuare con la migrazione? (s/N): " -n 1 -r
    echo

    if [[ ! $REPLY =~ ^[SsYy]$ ]]; then
        print_warning "Migrazione annullata dall'utente."
        exit 0
    fi
}

# ============================================================================
# STEP 1: BACKUP DATABASE
# ============================================================================

backup_database() {
    print_step "Backup del database..."

    local backup_file="bookrecommender_backup_$(date +%Y%m%d_%H%M%S).sql"

    echo "Eseguo backup del database in: $backup_file"

    if PGPASSWORD="$DB_PASSWORD" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" > "$backup_file"; then
        print_info "Backup completato con successo: $backup_file"
    else
        print_error "Backup fallito! Uscita."
        exit 1
    fi
}

# ============================================================================
# STEP 2: ANALIZZA DATABASE
# ============================================================================

analyze_database() {
    print_step "Analizza database per password in plaintext..."

    local query="
        SELECT COUNT(*) as plaintext_count
        FROM Users
        WHERE passwords NOT LIKE '\$2a\$%'
          AND passwords NOT LIKE '\$2b\$%'
          AND passwords NOT LIKE '\$2y\$%';
    "

    local result=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -A -c "$query")
    local plaintext_count=${result:-0}

    if [ "$plaintext_count" -eq 0 ]; then
        print_info "NESSUNA MIGRAZIONE NECESSARIA - Tutte le password sono già hashate!"
        return 1
    fi

    print_warning "Trovati $plaintext_count utenti con password in plaintext."
    return 0
}

# ============================================================================
# STEP 3: MIGRAZIONE PASSWORD
# ============================================================================

migrate_passwords() {
    print_step "Inizio migrazione password a BCrypt..."

    # Costruisci classpath per Java
    local classpath="target/classes:$(find ~/.m2/repository -name '*.jar' | tr '\n' ':')"

    # Query per ottenere utenti con password in plaintext
    local query="
        SELECT user_id, userid, passwords
        FROM Users
        WHERE passwords NOT LIKE '\$2a\$%'
          AND passwords NOT LIKE '\$2b\$%'
          AND passwords NOT LIKE '\$2y\$%';
    "

    # Ottieni dati utenti
    local users_data=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -A -F ',' -c "$query")

    if [ -z "$users_data" ]; then
        print_info "Nessun utente da migrare."
        return 0
    fi

    # Processa ogni utente
    local migrated_count=0
    IFS=',' read -r user_id userid password <<< "$users_data"

    # Crea comando Java per hashare password
    local hash_command="it.uninsubria.server.util.PasswordHashUtil.hashPassword('$password')"

    # Esegui comando Java per ottenere hash BCrypt
    local hashed_password=$(java -cp "$classpath" -c "$hash_command" 2>/dev/null)

    if [ -z "$hashed_password" ]; then
        print_error "Impossibile hashare password per utente: $userid"
        print_error "Verifica che il progetto sia compilato: mvn clean compile"
        continue
    fi

    # Aggiorna database con password hashata
    local update_query="
        UPDATE Users
        SET passwords = '$hashed_password'
        WHERE user_id = $user_id;
    "

    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "$update_query" > /dev/null 2>&1; then
        ((migrated_count++))
        print_info "Migrato: $userid ($migrated_count)"
    else
        print_error "Fallita migrazione per: $userid"
    fi

    print_info "Migrazione completata. Utenti migrati: $migrated_count"
}

# ============================================================================
# STEP 4: VERIFICAZIONE
# ============================================================================

verify_migration() {
    print_step "Verificazione migrazione..."

    local query="
        SELECT COUNT(*) as plaintext_count
        FROM Users
        WHERE passwords NOT LIKE '\$2a\$%'
          AND passwords NOT LIKE '\$2b\$%'
          AND passwords NOT LIKE '\$2y\$%';
    "

    local result=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -A -c "$query")
    local plaintext_count=${result:-0}

    if [ "$plaintext_count" -eq 0 ]; then
        print_info "✅ VERIFICA PASSATA: Nessuna password in plaintext rimanente!"
        return 0
    else
        print_warning "⚠️  ATTENZIONE: Ancora $plaintext_count password in plaintext!"
        print_warning "Verifica i log sopra per dettagli."
        return 1
    fi
}

# ============================================================================
# SCRIPT PRINCIPALE
# ============================================================================

main() {
    echo ""
    echo "=============================================="
    echo "  MIGRAZIONE PASSWORD BOOK RECOMMENDER  "
    echo "=============================================="
    echo ""

    # Verifica variabili d'ambiente
    if [ -z "$DB_PASSWORD" ]; then
        print_error "DB_PASSWORD non è impostata!"
        echo "Impostala con: export DB_PASSWORD='your_password'"
        exit 1
    fi

    # Verifica prerequisiti
    check_prerequisites

    # Conferma migrazione
    confirm_migration

    # Esegui backup
    backup_database

    # Analizza database
    if ! analyze_database; then
        print_info "Nessuna migrazione necessaria. Uscita."
        exit 0
    fi

    echo ""
    read -p "Premi Enter per continuare con la migrazione..." -r
    echo ""

    # Esegui migrazione
    migrate_passwords

    echo ""

    # Verifica migrazione
    verify_migration

    echo ""
    print_info "=============================================="
    print_info "  MIGRAZIONE COMPLETATA  "
    print_info "=============================================="
    echo ""
    print_info "Prossimi passi:"
    echo "1. Testare il login con gli utenti migrati"
    echo "2. Se i test hanno successo, l'applicazione rehasherà automaticamente le password al primo login"
    echo "3. Cancellare i file di backup dopo qualche giorno (se testati con successo)"
    echo ""
}

# Esegui script principale
main "$@"
