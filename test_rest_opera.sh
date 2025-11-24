#!/bin/bash
# Script de test des endpoints REST - Server Opera
# Port: 8084

echo "========================================="
echo "  TESTS REST - SERVER OPERA (Port 8084)"
echo "========================================="
echo ""

BASE_URL="http://localhost:8084"

# Couleurs pour l'affichage
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Ping
echo -e "${YELLOW}[TEST 1]${NC} Health Check - GET /api/ping"
RESPONSE=$(curl -s "$BASE_URL/api/ping")
if [ "$RESPONSE" = "pong" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Réponse: $RESPONSE"
else
    echo -e "${RED}✗ FAIL${NC} - Réponse: $RESPONSE"
fi
echo ""

# Test 2: Catalogue
echo -e "${YELLOW}[TEST 2]${NC} Catalogue - GET /api/hotels/catalog"
curl -s "$BASE_URL/api/hotels/catalog" | python3 -m json.tool
echo ""

# Test 3: Recherche d'offres (GET avec query params)
echo -e "${YELLOW}[TEST 3]${NC} Recherche offres - GET /api/hotels/search"
echo "Paramètres: ville=Montpellier, arrivee=2025-12-15, depart=2025-12-20, nbPersonnes=2"
curl -s "$BASE_URL/api/hotels/search?ville=Montpellier&arrivee=2025-12-15&depart=2025-12-20&nbPersonnes=2" | python3 -m json.tool
echo ""

# Test 4: Recherche d'offres (POST avec JSON)
echo -e "${YELLOW}[TEST 4]${NC} Recherche offres - POST /api/hotels/search (JSON)"
curl -s -X POST "$BASE_URL/api/hotels/search" \
  -H "Content-Type: application/json" \
  -d '{
    "ville": "Montpellier",
    "arrivee": "2025-12-25",
    "depart": "2025-12-28",
    "nbPersonnes": 2,
    "prixMin": 100,
    "prixMax": 500
  }' | python3 -m json.tool
echo ""

# Test 5: Réservation
echo -e "${YELLOW}[TEST 5]${NC} Réservation - POST /api/reservations"
RESERVATION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "offerId": "OF|luxeopera|202|2025-12-25|2025-12-28|2",
    "hotelName": "Luxe Opéra",
    "roomNumber": 202,
    "nom": "Martin",
    "prenom": "Sophie",
    "carte": "9876543210",
    "agence": "VoyagesPro",
    "arrivee": "2025-12-25",
    "depart": "2025-12-28"
  }')
echo "$RESERVATION_RESPONSE" | python3 -m json.tool

# Vérifier si la réservation a réussi
SUCCESS=$(echo "$RESERVATION_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('success', False))")
if [ "$SUCCESS" = "True" ]; then
    echo -e "${GREEN}✓ Réservation réussie${NC}"
else
    echo -e "${RED}✗ Réservation échouée${NC}"
fi
echo ""

# Test 6: Vérifier que la chambre n'est plus disponible
echo -e "${YELLOW}[TEST 6]${NC} Vérification disponibilité après réservation"
echo "Recherche de la chambre 202 pour les mêmes dates..."
SEARCH_RESULT=$(curl -s "$BASE_URL/api/hotels/search?ville=Montpellier&arrivee=2025-12-25&depart=2025-12-28&nbPersonnes=2")
echo "$SEARCH_RESULT" | python3 -m json.tool
ROOM_202_COUNT=$(echo "$SEARCH_RESULT" | python3 -c "import sys, json; data=json.load(sys.stdin); print(sum(1 for o in data['offers'] if o['roomNumber'] == 202))")
if [ "$ROOM_202_COUNT" = "0" ]; then
    echo -e "${GREEN}✓ Chambre 202 correctement marquée comme réservée${NC}"
else
    echo -e "${RED}✗ Chambre 202 toujours disponible (erreur)${NC}"
fi
echo ""

# Test 7: Gestion d'erreur - dates invalides
echo -e "${YELLOW}[TEST 7]${NC} Gestion d'erreur - dates invalides"
curl -s "$BASE_URL/api/hotels/search?ville=Montpellier&arrivee=2025-12-20&depart=2025-12-15&nbPersonnes=2" | python3 -m json.tool
echo ""

# Test 8: Gestion d'erreur - format de date incorrect
echo -e "${YELLOW}[TEST 8]${NC} Gestion d'erreur - format de date incorrect"
curl -s "$BASE_URL/api/hotels/search?ville=Montpellier&arrivee=2025/12/15&depart=2025/12/20&nbPersonnes=2" | python3 -m json.tool
echo ""

echo "========================================="
echo "  FIN DES TESTS"
echo "========================================="

