#!/bin/bash
# Script per rigenerare i pacchetti di distribuzione
# Esegue: build JAR → crea pacchetti → verifica

echo "🔨 Rigenerazione pacchetti di distribuzione..."
echo

# 1. Build completo del progetto
echo "📦 Building progetto..."
mvn clean package -q
if [ $? -ne 0 ]; then
    echo "❌ Build fallito!"
    exit 1
fi
echo "✅ Build completato"
echo

# 2. Preparazione directory distribuzione
echo "📁 Preparazione distribuzione..."
mkdir -p dist
cp target/launcher-1.0-SNAPSHOT-jar-with-dependencies.jar dist/BookRecommender.jar
echo "✅ JAR copiato"
echo

# 3. Creazione pacchetti
echo "📦 Creazione pacchetti..."
cd dist

# Sostituisci pacchetti esistenti
rm -f ../BookRecommender-*.tar.gz ../BookRecommender-*.zip

# Crea nuovi pacchetti
tar -czf ../BookRecommender-Unix.tar.gz *
zip -r ../BookRecommender-Windows.zip *

cd ..
echo "✅ Pacchetti creati:"
ls -lh BookRecommender-*.tar.gz BookRecommender-*.zip
echo

# 4. Test rapido
echo "🧪 Test rapido pacchetti..."
if [ -f "BookRecommender-Unix.tar.gz" ] && [ -f "BookRecommender-Windows.zip" ]; then
    echo "✅ Tutti i pacchetti sono stati creati correttamente!"
    echo
    echo "🎯 Pronti per GitHub Release!"
    echo "   - Carica: BookRecommender-Windows.zip"
    echo "   - Carica: BookRecommender-Unix.tar.gz"
else
    echo "❌ Errore nella creazione dei pacchetti!"
    exit 1
fi

# git tag v1.1.0, git push origin v1.1.0 -> ovviamnete in base alla versione che vuoi mettere