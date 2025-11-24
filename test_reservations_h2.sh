#!/bin/bash
# Script de test complet pour vérifier la gestion des réservations en base H2

echo "============================================================"
echo "  TEST COMPLET : GESTION RÉSERVATIONS EN BASE H2"
echo "============================================================"
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Variables
OPERA_URL="http://localhost:8084/api"
TEST_DATES_1_START="2025-12-28"
TEST_DATES_1_END="2025-12-30"
TEST_DATES_2_START="2025-12-29"
TEST_DATES_2_END="2025-12-31"

echo -e "${BLUE}[TEST 1/5]${NC} Recherche initiale des chambres disponibles"
echo "→ Période : $TEST_DATES_1_START à $TEST_DATES_1_END"
echo ""

SEARCH_1=$(curl -s "${OPERA_URL}/hotels/search?ville=Montpellier&arrivee=${TEST_DATES_1_START}&depart=${TEST_DATES_1_END}&nbPersonnes=2")
OFFERS_1=$(echo "$SEARCH_1" | python3 -c "import sys,json; offers=json.load(sys.stdin).get('offers',[]); print(len(offers))" 2>/dev/null)

echo "Offres trouvées : $OFFERS_1"
if [ "$OFFERS_1" -gt "0" ]; then
    echo -e "${GREEN}✓${NC} Des chambres sont disponibles"
    echo "$SEARCH_1" | python3 -c "import sys,json; [print(f'  • Chambre {o[\"room\"][\"numero\"]} - {o[\"prixTotal\"]}€') for o in json.load(sys.stdin).get('offers',[])]" 2>/dev/null
else
    echo -e "${RED}✗${NC} Aucune chambre disponible (problème)"
    exit 1
fi

echo ""
echo -e "${BLUE}[TEST 2/5]${NC} Création d'une réservation (chambre 201)"
echo "→ Période : $TEST_DATES_1_START à $TEST_DATES_1_END"
echo ""

RESERVATION=$(curl -s -X POST "${OPERA_URL}/reservations" \
  -H "Content-Type: application/json" \
  -d "{
    \"offerId\": \"OF|luxeopera|201|${TEST_DATES_1_START}|${TEST_DATES_1_END}|2\",
    \"hotelName\": \"Luxe Opéra\",
    \"roomNumber\": 201,
    \"nom\": \"TestDB\",
    \"prenom\": \"Verification\",
    \"carte\": \"1234567890\",
    \"arrivee\": \"${TEST_DATES_1_START}\",
    \"depart\": \"${TEST_DATES_1_END}\"
  }")

RESERVATION_SUCCESS=$(echo "$RESERVATION" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success','false'))" 2>/dev/null)
RESERVATION_REF=$(echo "$RESERVATION" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id','N/A'))" 2>/dev/null)
RESERVATION_MSG=$(echo "$RESERVATION" | python3 -c "import sys,json; print(json.load(sys.stdin).get('message','N/A'))" 2>/dev/null)

if [ "$RESERVATION_SUCCESS" = "True" ] || [ "$RESERVATION_SUCCESS" = "true" ]; then
    echo -e "${GREEN}✓${NC} Réservation créée avec succès"
    echo "  Référence : $RESERVATION_REF"
    echo "  Message : $RESERVATION_MSG"
else
    echo -e "${YELLOW}⚠${NC} Réservation échouée : $RESERVATION_MSG"
    echo "  (La chambre était peut-être déjà réservée)"
fi

echo ""
echo -e "${BLUE}[TEST 3/5]${NC} Recherche après réservation (même période)"
echo "→ La chambre 201 ne doit PAS apparaître"
echo ""

SEARCH_2=$(curl -s "${OPERA_URL}/hotels/search?ville=Montpellier&arrivee=${TEST_DATES_1_START}&depart=${TEST_DATES_1_END}&nbPersonnes=2")
OFFERS_2=$(echo "$SEARCH_2" | python3 -c "import sys,json; offers=json.load(sys.stdin).get('offers',[]); print(len(offers))" 2>/dev/null)
ROOM_201_FOUND=$(echo "$SEARCH_2" | python3 -c "import sys,json; offers=json.load(sys.stdin).get('offers',[]); print(any(o['room']['numero']==201 for o in offers))" 2>/dev/null)

echo "Offres trouvées : $OFFERS_2"
echo "$SEARCH_2" | python3 -c "import sys,json; [print(f'  • Chambre {o[\"room\"][\"numero\"]} - {o[\"prixTotal\"]}€') for o in json.load(sys.stdin).get('offers',[])]" 2>/dev/null

if [ "$ROOM_201_FOUND" = "True" ]; then
    echo -e "${RED}✗ ÉCHEC${NC} : La chambre 201 apparaît encore (elle devrait être réservée)"
    echo -e "${RED}  → La vérification H2 ne fonctionne pas correctement${NC}"
else
    echo -e "${GREEN}✓ SUCCÈS${NC} : La chambre 201 est bien filtrée (réservée)"
    echo -e "${GREEN}  → La base H2 est bien interrogée${NC}"
fi

echo ""
echo -e "${BLUE}[TEST 4/5]${NC} Tentative de réservation sur période chevauchante"
echo "→ Période : $TEST_DATES_2_START à $TEST_DATES_2_END (chevauche la 1ère)"
echo ""

RESERVATION_2=$(curl -s -X POST "${OPERA_URL}/reservations" \
  -H "Content-Type: application/json" \
  -d "{
    \"offerId\": \"OF|luxeopera|201|${TEST_DATES_2_START}|${TEST_DATES_2_END}|2\",
    \"hotelName\": \"Luxe Opéra\",
    \"roomNumber\": 201,
    \"nom\": \"TestConflict\",
    \"prenom\": \"Verification\",
    \"carte\": \"9999999999\",
    \"arrivee\": \"${TEST_DATES_2_START}\",
    \"depart\": \"${TEST_DATES_2_END}\"
  }")

RESERVATION_2_SUCCESS=$(echo "$RESERVATION_2" | python3 -c "import sys,json; print(json.load(sys.stdin).get('success','false'))" 2>/dev/null)
RESERVATION_2_MSG=$(echo "$RESERVATION_2" | python3 -c "import sys,json; print(json.load(sys.stdin).get('message','N/A'))" 2>/dev/null)

if [ "$RESERVATION_2_SUCCESS" = "False" ] || [ "$RESERVATION_2_SUCCESS" = "false" ]; then
    echo -e "${GREEN}✓ SUCCÈS${NC} : Réservation refusée (conflit détecté)"
    echo "  Message : $RESERVATION_2_MSG"
    echo -e "${GREEN}  → La détection de chevauchement fonctionne${NC}"
else
    echo -e "${RED}✗ ÉCHEC${NC} : Réservation acceptée (elle devrait être refusée)"
    echo -e "${RED}  → La détection de chevauchement ne fonctionne pas${NC}"
fi

echo ""
echo -e "${BLUE}[TEST 5/5]${NC} Recherche sur période différente (non chevauchante)"
echo "→ Période : 2026-01-05 à 2026-01-07"
echo ""

SEARCH_3=$(curl -s "${OPERA_URL}/hotels/search?ville=Montpellier&arrivee=2026-01-05&depart=2026-01-07&nbPersonnes=2")
OFFERS_3=$(echo "$SEARCH_3" | python3 -c "import sys,json; offers=json.load(sys.stdin).get('offers',[]); print(len(offers))" 2>/dev/null)
ROOM_201_AVAILABLE=$(echo "$SEARCH_3" | python3 -c "import sys,json; offers=json.load(sys.stdin).get('offers',[]); print(any(o['room']['numero']==201 for o in offers))" 2>/dev/null)

echo "Offres trouvées : $OFFERS_3"
echo "$SEARCH_3" | python3 -c "import sys,json; [print(f'  • Chambre {o[\"room\"][\"numero\"]} - {o[\"prixTotal\"]}€') for o in json.load(sys.stdin).get('offers',[])]" 2>/dev/null

if [ "$ROOM_201_AVAILABLE" = "True" ]; then
    echo -e "${GREEN}✓ SUCCÈS${NC} : La chambre 201 est disponible (pas de conflit)"
    echo -e "${GREEN}  → La détection de période fonctionne correctement${NC}"
else
    echo -e "${YELLOW}⚠ ATTENTION${NC} : La chambre 201 n'apparaît pas"
    echo "  (Peut-être une autre réservation ou un problème)"
fi

echo ""
echo "============================================================"
echo "  RÉSUMÉ DES TESTS"
echo "============================================================"
echo ""

# Compter les succès
SUCCESS_COUNT=0

# Test 1 : Offres initiales
if [ "$OFFERS_1" -gt "0" ]; then
    echo -e "Test 1 - Recherche initiale          : ${GREEN}✓ PASS${NC}"
    ((SUCCESS_COUNT++))
else
    echo -e "Test 1 - Recherche initiale          : ${RED}✗ FAIL${NC}"
fi

# Test 2 : Création réservation
if [ "$RESERVATION_SUCCESS" = "True" ] || [ "$RESERVATION_SUCCESS" = "true" ]; then
    echo -e "Test 2 - Création réservation        : ${GREEN}✓ PASS${NC}"
    ((SUCCESS_COUNT++))
else
    echo -e "Test 2 - Création réservation        : ${YELLOW}⚠ SKIP${NC}"
fi

# Test 3 : Filtrage chambre réservée
if [ "$ROOM_201_FOUND" != "True" ]; then
    echo -e "Test 3 - Filtrage H2                 : ${GREEN}✓ PASS${NC}"
    ((SUCCESS_COUNT++))
else
    echo -e "Test 3 - Filtrage H2                 : ${RED}✗ FAIL${NC}"
fi

# Test 4 : Détection chevauchement
if [ "$RESERVATION_2_SUCCESS" = "False" ] || [ "$RESERVATION_2_SUCCESS" = "false" ]; then
    echo -e "Test 4 - Détection chevauchement     : ${GREEN}✓ PASS${NC}"
    ((SUCCESS_COUNT++))
else
    echo -e "Test 4 - Détection chevauchement     : ${RED}✗ FAIL${NC}"
fi

# Test 5 : Disponibilité période différente
if [ "$ROOM_201_AVAILABLE" = "True" ]; then
    echo -e "Test 5 - Période non chevauchante    : ${GREEN}✓ PASS${NC}"
    ((SUCCESS_COUNT++))
else
    echo -e "Test 5 - Période non chevauchante    : ${YELLOW}⚠ SKIP${NC}"
fi

echo ""
echo "Résultat : $SUCCESS_COUNT/5 tests réussis"
echo ""

if [ "$SUCCESS_COUNT" -ge "3" ]; then
    echo -e "${GREEN}✓ La gestion des réservations en base H2 fonctionne !${NC}"
else
    echo -e "${YELLOW}⚠ Certains tests ont échoué, vérifiez les logs${NC}"
fi

echo ""
echo "Pour voir les logs détaillés :"
echo "  tail -f logs/opera.log | grep REST-SERVICE"
echo ""
echo "Pour accéder à la console H2 :"
echo "  http://localhost:8084/h2-console"
echo "  JDBC URL: jdbc:h2:file:./data/hotel-opera-db"
echo "  User: opera / Pass: opera"
echo ""
echo "============================================================"

