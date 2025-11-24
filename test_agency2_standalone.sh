#!/bin/bash

# Script de test standalone de l'agence 2

echo "=========================================="
echo "  TEST AGENCE 2 STANDALONE"
echo "=========================================="
echo ""

echo "1. Compilation de l'agence 2..."
cd /home/etudiant/Bureau/Rest/HotelRest/agency-server-2
mvn clean install -DskipTests > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo "   ✅ Compilation réussie"
else
    echo "   ❌ Erreur de compilation"
    exit 1
fi

echo ""
echo "2. Démarrage de l'agence 2 (port TCP 7071, HTTP 8081)..."
mvn spring-boot:run > /tmp/agency2_test.log 2>&1 &
PID=$!
echo "   PID: $PID"

echo ""
echo "3. Attente du démarrage (15 secondes)..."
sleep 15

echo ""
echo "4. Vérification du port TCP 7071..."
if echo '{"op":"ping"}' | nc -w 2 localhost 7071 2>/dev/null | grep -q "pong"; then
    echo "   ✅ Port TCP 7071 répond"
else
    echo "   ❌ Port TCP 7071 ne répond pas"
    echo ""
    echo "Logs:"
    tail -30 /tmp/agency2_test.log
    kill $PID 2>/dev/null
    exit 1
fi

echo ""
echo "5. Test catalogue..."
CATALOG=$(echo '{"op":"catalog.get"}' | nc -w 2 localhost 7071 2>/dev/null)
NAME=$(echo "$CATALOG" | python3 -c "import sys, json; print(json.load(sys.stdin).get('data', {}).get('name', 'N/A'))" 2>/dev/null)
DISCOUNT=$(echo "$CATALOG" | python3 -c "import sys, json; print(json.load(sys.stdin).get('data', {}).get('discount', 'N/A'))" 2>/dev/null)

echo "   Nom: $NAME"
echo "   Réduction: $DISCOUNT"

if [ "$NAME" = "SuperAgence" ] && [ "$DISCOUNT" = "20%" ]; then
    echo "   ✅ Configuration correcte"
else
    echo "   ❌ Configuration incorrecte"
fi

echo ""
echo "6. Arrêt de l'agence 2..."
kill $PID 2>/dev/null
sleep 2

echo ""
echo "=========================================="
echo "  TEST TERMINÉ"
echo "=========================================="

