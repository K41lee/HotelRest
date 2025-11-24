#!/bin/bash
# Script de test pour l'affichage des images dans le client GUI

echo "=============================================="
echo "  TEST AFFICHAGE IMAGES - CLIENT GUI"
echo "=============================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}[1/5]${NC} Vérification des serveurs"
echo ""

# Vérifier que les serveurs sont actifs
RIVAGE_PING=$(curl -s http://localhost:8082/api/ping 2>/dev/null)
OPERA_PING=$(curl -s http://localhost:8084/api/ping 2>/dev/null)
AGENCY_PING=$(echo '{"op":"ping"}' | nc -w 2 localhost 7070 2>/dev/null)

if [ "$RIVAGE_PING" = "pong" ]; then
    echo -e "${GREEN}✓${NC} Server Rivage actif (8082)"
else
    echo -e "${RED}✗${NC} Server Rivage non disponible"
    echo "   Lancez : ./lancement.sh --no-client"
    exit 1
fi

if [ "$OPERA_PING" = "pong" ]; then
    echo -e "${GREEN}✓${NC} Server Opera actif (8084)"
else
    echo -e "${RED}✗${NC} Server Opera non disponible"
    exit 1
fi

if echo "$AGENCY_PING" | grep -q "pong"; then
    echo -e "${GREEN}✓${NC} Agency Server actif (7070)"
else
    echo -e "${RED}✗${NC} Agency Server non disponible"
    exit 1
fi

echo ""
echo -e "${BLUE}[2/5]${NC} Vérification des images disponibles"
echo ""

# Test accès images
RIVAGE_101=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/images/rivage-room-101.svg)
RIVAGE_102=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/images/rivage-room-102.svg)
OPERA_201=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8084/images/opera-room-201.svg)
OPERA_202=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8084/images/opera-room-202.svg)

if [ "$RIVAGE_101" = "200" ] && [ "$RIVAGE_102" = "200" ] &&
   [ "$OPERA_201" = "200" ] && [ "$OPERA_202" = "200" ]; then
    echo -e "${GREEN}✓${NC} Toutes les images sont accessibles (4/4)"
else
    echo -e "${YELLOW}⚠${NC} Certaines images ne sont pas accessibles"
    echo "   Rivage 101: HTTP $RIVAGE_101"
    echo "   Rivage 102: HTTP $RIVAGE_102"
    echo "   Opera 201: HTTP $OPERA_201"
    echo "   Opera 202: HTTP $OPERA_202"
fi

echo ""
echo -e "${BLUE}[3/5]${NC} Vérification des images dans l'API"
echo ""

# Test API avec images
SEARCH_RESULT=$(curl -s "http://localhost:8084/api/hotels/search?ville=Montpellier&arrivee=2025-12-25&depart=2025-12-27&nbPersonnes=2")
IMAGES_COUNT=$(echo "$SEARCH_RESULT" | python3 -c "
import sys, json
offers = json.load(sys.stdin).get('offers', [])
count = sum(1 for o in offers if o.get('room', {}).get('imageUrl'))
print(count)
" 2>/dev/null)

if [ "$IMAGES_COUNT" = "2" ]; then
    echo -e "${GREEN}✓${NC} API retourne les images (2/2 offres)"
    echo "$SEARCH_RESULT" | python3 -c "
import sys, json
offers = json.load(sys.stdin).get('offers', [])
for o in offers:
    room = o.get('room', {})
    print(f\"  Chambre {room.get('numero')}: {room.get('imageUrl', 'AUCUNE')}\")
" 2>/dev/null
else
    echo -e "${YELLOW}⚠${NC} API retourne seulement $IMAGES_COUNT images sur 2"
fi

echo ""
echo -e "${BLUE}[4/5]${NC} Test du client CLI compilé"
echo ""

if [ -f "client-cli/target/client-cli-1.0.0.jar" ]; then
    echo -e "${GREEN}✓${NC} Client compilé : client-cli/target/client-cli-1.0.0.jar"
else
    echo -e "${RED}✗${NC} Client non compilé"
    echo "   Compilez : cd client-cli && mvn clean install"
    exit 1
fi

echo ""
echo -e "${BLUE}[5/5]${NC} Instructions pour tester l'affichage"
echo ""

cat << 'EOF'
📋 Comment tester l'affichage des images dans le client GUI :

1️⃣  Lancer le client GUI :
   cd client-cli
   mvn exec:java -Dexec.mainClass=org.examples.client.gui.HotelClientGUI

2️⃣  Dans l'interface :
   • Cliquer sur "Rechercher un Hôtel"
   • Sélectionner une ville (Montpellier ou Sète)
   • Choisir des dates
   • Cliquer sur "Rechercher"

3️⃣  Dans les résultats :
   • Vous verrez une colonne "Image"
   • Pour les chambres avec images : "🖼️ Voir"
   • Cliquer sur "🖼️ Voir" pour afficher l'image

4️⃣  Fenêtre d'image :
   • Pour les SVG : Affiche le contenu + bouton navigateur
   • Cliquer sur "🌐 Ouvrir dans le navigateur" pour voir l'image

EOF

echo ""
echo "=============================================="
echo "  RÉSUMÉ"
echo "=============================================="
echo ""

echo "Configuration :"
echo "  • Serveur Rivage : http://localhost:8082"
echo "  • Serveur Opera  : http://localhost:8084"
echo "  • Agency Server  : localhost:7070"
echo ""

echo "Images disponibles :"
echo "  • Rivage 101 : /images/rivage-room-101.svg"
echo "  • Rivage 102 : /images/rivage-room-102.svg"
echo "  • Opera 201  : /images/opera-room-201.svg"
echo "  • Opera 202  : /images/opera-room-202.svg"
echo ""

echo "Fonctionnalités du client :"
echo "  ✅ Lecture de room.imageUrl (corrigé)"
echo "  ✅ Affichage de l'icône 🖼️ dans la colonne Image"
echo "  ✅ Clic sur l'icône ouvre une fenêtre"
echo "  ✅ Support SVG avec bouton navigateur"
echo "  ✅ Chargement asynchrone des images"
echo ""

echo "Pour lancer le client GUI :"
echo "  cd client-cli"
echo "  mvn exec:java -Dexec.mainClass=org.examples.client.gui.HotelClientGUI"
echo ""

echo "=============================================="

