#!/bin/bash
# Script de vérification des images en base de données H2

echo "=============================================="
echo "  VÉRIFICATION IMAGES EN BASE H2"
echo "=============================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}[1/3]${NC} Vérification via l'API REST"
echo ""

# Test Opera
echo "→ Opera (Montpellier) :"
OPERA_RESULT=$(curl -s "http://localhost:8084/api/hotels/search?ville=Montpellier&arrivee=2025-12-25&depart=2025-12-27&nbPersonnes=2")
OPERA_IMAGES=$(echo "$OPERA_RESULT" | python3 -c "
import sys, json
offers = json.load(sys.stdin).get('offers', [])
for o in offers:
    room = o.get('room', {})
    print(f'  Chambre {room.get(\"numero\")}: {room.get(\"imageUrl\", \"AUCUNE\")}')
" 2>/dev/null)

if [ -n "$OPERA_IMAGES" ]; then
    echo "$OPERA_IMAGES"
    echo -e "${GREEN}✓${NC} Opera : Images présentes dans l'API"
else
    echo -e "${RED}✗${NC} Opera : Aucune image trouvée"
fi

echo ""

# Test Rivage
echo "→ Rivage (Sète) :"
RIVAGE_RESULT=$(curl -s "http://localhost:8082/api/hotels/search?ville=Sete&arrivee=2025-12-25&depart=2025-12-27&nbPersonnes=2")
RIVAGE_IMAGES=$(echo "$RIVAGE_RESULT" | python3 -c "
import sys, json
offers = json.load(sys.stdin).get('offers', [])
for o in offers:
    room = o.get('room', {})
    print(f'  Chambre {room.get(\"numero\")}: {room.get(\"imageUrl\", \"AUCUNE\")}')
" 2>/dev/null)

if [ -n "$RIVAGE_IMAGES" ]; then
    echo "$RIVAGE_IMAGES"
    echo -e "${GREEN}✓${NC} Rivage : Images présentes dans l'API"
else
    echo -e "${RED}✗${NC} Rivage : Aucune image trouvée"
fi

echo ""
echo -e "${BLUE}[2/3]${NC} Requêtes SQL directes (simulation)"
echo ""

echo "→ SQL pour consulter les chambres :"
echo '  SELECT NUMERO, NB_LITS, PRIX_PAR_NUIT, IMAGE_URL FROM CHAMBRE;'
echo ""
echo "Pour exécuter cette requête :"
echo "  1. Ouvrir http://localhost:8082/h2-console (Rivage)"
echo "  2. JDBC URL: jdbc:h2:file:./data/hotel-rivage-db"
echo "  3. User: rivage / Pass: rivage"
echo "  4. Exécuter la requête ci-dessus"
echo ""

echo -e "${BLUE}[3/3]${NC} Résumé"
echo ""

# Compter les images non-null
OPERA_COUNT=$(echo "$OPERA_RESULT" | python3 -c "
import sys, json
offers = json.load(sys.stdin).get('offers', [])
count = sum(1 for o in offers if o.get('room', {}).get('imageUrl'))
print(count)
" 2>/dev/null)

RIVAGE_COUNT=$(echo "$RIVAGE_RESULT" | python3 -c "
import sys, json
offers = json.load(sys.stdin).get('offers', [])
count = sum(1 for o in offers if o.get('room', {}).get('imageUrl'))
print(count)
" 2>/dev/null)

echo "Chambres avec images :"
echo "  • Opera  : $OPERA_COUNT/2"
echo "  • Rivage : $RIVAGE_COUNT/2"
echo ""

TOTAL_WITH_IMAGES=$((OPERA_COUNT + RIVAGE_COUNT))
if [ "$TOTAL_WITH_IMAGES" -eq 4 ]; then
    echo -e "${GREEN}✓ Toutes les chambres ont des images !${NC}"
elif [ "$TOTAL_WITH_IMAGES" -gt 0 ]; then
    echo -e "${YELLOW}⚠ Certaines chambres ont des images ($TOTAL_WITH_IMAGES/4)${NC}"
else
    echo -e "${RED}✗ Aucune chambre n'a d'image${NC}"
fi

echo ""
echo "Pour voir les images :"
echo "  ./test_images_chambres.sh"
echo ""
echo "=============================================="

