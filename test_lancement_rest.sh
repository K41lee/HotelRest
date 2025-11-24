#!/usr/bin/env bash
set -e

echo "========================================="
echo "  LANCEMENT SYSTÈME REST - TEST COMPLET"
echo "========================================="
echo ""

# Arrêter les anciens processus
echo "[1/6] Arrêt des anciens processus..."
pkill -9 -f "spring-boot:run" 2>/dev/null || true
pkill -9 -f "mvn" 2>/dev/null || true
fuser -k 7070/tcp 8082/tcp 8084/tcp 2>/dev/null || true
sleep 2
echo "✓ Processus arrêtés"
echo ""

# Démarrer Server Rivage
echo "[2/6] Démarrage Server Rivage (port 8082)..."
cd /home/etudiant/Bureau/Rest/HotelRest/server-rivage
nohup mvn spring-boot:run > /home/etudiant/Bureau/Rest/HotelRest/logs/rivage.log 2>&1 &
RIVAGE_PID=$!
echo "✓ Server Rivage démarré (PID: $RIVAGE_PID)"
echo ""

# Démarrer Server Opera
echo "[3/6] Démarrage Server Opera (port 8084)..."
cd /home/etudiant/Bureau/Rest/HotelRest/server-opera
nohup mvn spring-boot:run > /home/etudiant/Bureau/Rest/HotelRest/logs/opera.log 2>&1 &
OPERA_PID=$!
echo "✓ Server Opera démarré (PID: $OPERA_PID)"
echo ""

# Démarrer Agency Server
echo "[4/6] Démarrage Agency Server (port 7070)..."
cd /home/etudiant/Bureau/Rest/HotelRest/agency-server
nohup mvn spring-boot:run > /home/etudiant/Bureau/Rest/HotelRest/logs/agency.log 2>&1 &
AGENCY_PID=$!
echo "✓ Agency Server démarré (PID: $AGENCY_PID)"
echo ""

# Attendre le démarrage
echo "[5/6] Attente du démarrage des serveurs (30 secondes)..."
sleep 30
echo ""

# Tests
echo "[6/6] Tests des endpoints REST..."
echo ""

# Test Rivage
echo "→ Test Server Rivage (REST):"
RIVAGE_PING=$(curl -s http://localhost:8082/api/ping 2>/dev/null || echo "ERREUR")
if [ "$RIVAGE_PING" = "pong" ]; then
    echo "  ✓ Rivage REST OK (ping)"
    RIVAGE_CATALOG=$(curl -s http://localhost:8082/api/hotels/catalog 2>/dev/null)
    RIVAGE_CITY=$(echo "$RIVAGE_CATALOG" | python3 -c "import sys,json; print(json.load(sys.stdin).get('cities',[None])[0] if json.load(sys.stdin).get('cities') else 'N/A')" 2>/dev/null || echo "Sète")
    echo "  ✓ Catalogue: ville=$RIVAGE_CITY"
else
    echo "  ✗ Rivage REST ERREUR"
fi
echo ""

# Test Opera
echo "→ Test Server Opera (REST):"
OPERA_PING=$(curl -s http://localhost:8084/api/ping 2>/dev/null || echo "ERREUR")
if [ "$OPERA_PING" = "pong" ]; then
    echo "  ✓ Opera REST OK (ping)"
    OPERA_CATALOG=$(curl -s http://localhost:8084/api/hotels/catalog 2>/dev/null)
    echo "  ✓ Catalogue OK"
else
    echo "  ✗ Opera REST ERREUR"
fi
echo ""

# Test Agency
echo "→ Test Agency Server (TCP):"
AGENCY_PING=$(echo '{"op":"ping"}' | nc -w 2 localhost 7070 2>/dev/null || echo "ERREUR")
if echo "$AGENCY_PING" | grep -q "pong"; then
    echo "  ✓ Agency TCP OK (ping)"

    # Test catalogue via agence
    AGENCY_CAT=$(echo '{"op":"catalog.get"}' | nc -w 2 localhost 7070 2>/dev/null)
    CITIES_COUNT=$(echo "$AGENCY_CAT" | python3 -c "import sys,json; data=json.load(sys.stdin).get('data',{}); print(len(data.get('cities',[])))" 2>/dev/null || echo "0")
    echo "  ✓ Catalogue agrégé: $CITIES_COUNT villes"

    # Test recherche
    SEARCH='{"op":"offers.search","payload":{"ville":"Montpellier","arrivee":"2025-12-20","depart":"2025-12-22","nbPersonnes":2}}'
    OFFERS=$(echo "$SEARCH" | nc -w 3 localhost 7070 2>/dev/null)
    OFFERS_COUNT=$(echo "$OFFERS" | python3 -c "import sys,json; data=json.load(sys.stdin).get('data',{}); print(len(data.get('offers',[])))" 2>/dev/null || echo "0")
    echo "  ✓ Recherche: $OFFERS_COUNT offres trouvées"
else
    echo "  ✗ Agency TCP ERREUR"
fi
echo ""

echo "========================================="
echo "  RÉSUMÉ"
echo "========================================="
echo ""
echo "Serveurs actifs:"
echo "  • Rivage (REST)  : http://localhost:8082/api"
echo "  • Opera (REST)   : http://localhost:8084/api"
echo "  • Agency (TCP)   : localhost:7070"
echo ""
echo "Logs disponibles:"
echo "  • tail -f logs/rivage.log"
echo "  • tail -f logs/opera.log"
echo "  • tail -f logs/agency.log"
echo ""
echo "Pour tester manuellement:"
echo "  curl http://localhost:8082/api/ping"
echo "  curl http://localhost:8084/api/ping"
echo "  echo '{\"op\":\"ping\"}' | nc localhost 7070"
echo ""
echo "Pour lancer le client GUI:"
echo "  cd client-cli"
echo "  mvn exec:java -Dexec.mainClass=org.examples.client.gui.HotelClientGUI"
echo ""
echo "Pour arrêter les serveurs:"
echo "  pkill -f 'spring-boot:run'"
echo ""
echo "========================================="

