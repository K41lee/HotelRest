#!/bin/bash
# Script de test pour vérifier que les images des chambres sont bien disponibles

echo "=============================================="
echo "  TEST DES IMAGES DES CHAMBRES"
echo "=============================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}[TEST 1/4]${NC} Vérification des fichiers images"
echo ""

# Vérifier les images Opera
if [ -f "server-opera/src/main/resources/static/images/opera-room-201.svg" ]; then
    echo -e "${GREEN}✓${NC} Opera chambre 201 : Fichier présent"
else
    echo -e "${RED}✗${NC} Opera chambre 201 : Fichier manquant"
fi

if [ -f "server-opera/src/main/resources/static/images/opera-room-202.svg" ]; then
    echo -e "${GREEN}✓${NC} Opera chambre 202 : Fichier présent"
else
    echo -e "${RED}✗${NC} Opera chambre 202 : Fichier manquant"
fi

# Vérifier les images Rivage
if [ -f "server-rivage/src/main/resources/static/images/rivage-room-101.svg" ]; then
    echo -e "${GREEN}✓${NC} Rivage chambre 101 : Fichier présent"
else
    echo -e "${RED}✗${NC} Rivage chambre 101 : Fichier manquant"
fi

if [ -f "server-rivage/src/main/resources/static/images/rivage-room-102.svg" ]; then
    echo -e "${GREEN}✓${NC} Rivage chambre 102 : Fichier présent"
else
    echo -e "${RED}✗${NC} Rivage chambre 102 : Fichier manquant"
fi

echo ""
echo -e "${BLUE}[TEST 2/4]${NC} Test d'accès HTTP aux images"
echo ""

# Test accès images Opera
OPERA_201=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8084/images/opera-room-201.svg)
if [ "$OPERA_201" = "200" ]; then
    echo -e "${GREEN}✓${NC} Opera 201 accessible (HTTP 200)"
else
    echo -e "${RED}✗${NC} Opera 201 non accessible (HTTP $OPERA_201)"
fi

OPERA_202=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8084/images/opera-room-202.svg)
if [ "$OPERA_202" = "200" ]; then
    echo -e "${GREEN}✓${NC} Opera 202 accessible (HTTP 200)"
else
    echo -e "${RED}✗${NC} Opera 202 non accessible (HTTP $OPERA_202)"
fi

# Test accès images Rivage
RIVAGE_101=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/images/rivage-room-101.svg)
if [ "$RIVAGE_101" = "200" ]; then
    echo -e "${GREEN}✓${NC} Rivage 101 accessible (HTTP 200)"
else
    echo -e "${RED}✗${NC} Rivage 101 non accessible (HTTP $RIVAGE_101)"
fi

RIVAGE_102=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/images/rivage-room-102.svg)
if [ "$RIVAGE_102" = "200" ]; then
    echo -e "${GREEN}✓${NC} Rivage 102 accessible (HTTP 200)"
else
    echo -e "${RED}✗${NC} Rivage 102 non accessible (HTTP $RIVAGE_102)"
fi

echo ""
echo -e "${BLUE}[TEST 3/4]${NC} Vérification API REST (imageUrl inclus)"
echo ""

# Test API Opera
SEARCH_OPERA=$(curl -s "http://localhost:8084/api/hotels/search?ville=Montpellier&arrivee=2025-12-25&depart=2025-12-27&nbPersonnes=2")
IMAGE_201=$(echo "$SEARCH_OPERA" | python3 -c "import sys,json; offers=json.load(sys.stdin).get('offers',[]); print([o['room'].get('imageUrl','') for o in offers if o['room']['numero']==201][0] if offers else '')" 2>/dev/null)
IMAGE_202=$(echo "$SEARCH_OPERA" | python3 -c "import sys,json; offers=json.load(sys.stdin).get('offers',[]); print([o['room'].get('imageUrl','') for o in offers if o['room']['numero']==202][0] if offers else '')" 2>/dev/null)

if [ -n "$IMAGE_201" ]; then
    echo -e "${GREEN}✓${NC} Opera API - Chambre 201 : imageUrl=$IMAGE_201"
else
    echo -e "${RED}✗${NC} Opera API - Chambre 201 : imageUrl manquant"
fi

if [ -n "$IMAGE_202" ]; then
    echo -e "${GREEN}✓${NC} Opera API - Chambre 202 : imageUrl=$IMAGE_202"
else
    echo -e "${RED}✗${NC} Opera API - Chambre 202 : imageUrl manquant"
fi

# Test API Rivage
SEARCH_RIVAGE=$(curl -s "http://localhost:8082/api/hotels/search?ville=Sete&arrivee=2025-12-25&depart=2025-12-27&nbPersonnes=2")
IMAGE_101=$(echo "$SEARCH_RIVAGE" | python3 -c "import sys,json; offers=json.load(sys.stdin).get('offers',[]); print([o['room'].get('imageUrl','') for o in offers if o['room']['numero']==101][0] if offers else '')" 2>/dev/null)
IMAGE_102=$(echo "$SEARCH_RIVAGE" | python3 -c "import sys,json; offers=json.load(sys.stdin).get('offers',[]); print([o['room'].get('imageUrl','') for o in offers if o['room']['numero']==102][0] if offers else '')" 2>/dev/null)

if [ -n "$IMAGE_101" ]; then
    echo -e "${GREEN}✓${NC} Rivage API - Chambre 101 : imageUrl=$IMAGE_101"
else
    echo -e "${RED}✗${NC} Rivage API - Chambre 101 : imageUrl manquant"
fi

if [ -n "$IMAGE_102" ]; then
    echo -e "${GREEN}✓${NC} Rivage API - Chambre 102 : imageUrl=$IMAGE_102"
else
    echo -e "${RED}✗${NC} Rivage API - Chambre 102 : imageUrl manquant"
fi

echo ""
echo -e "${BLUE}[TEST 4/4]${NC} Génération page HTML de visualisation"
echo ""

cat > /tmp/test_images_chambres.html << 'HTMLEOF'
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>Images des Chambres</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        h1 { color: #333; }
        .hotel { margin-bottom: 40px; background: white; padding: 20px; border-radius: 10px; }
        .rooms { display: flex; gap: 20px; flex-wrap: wrap; }
        .room { border: 2px solid #ddd; padding: 15px; border-radius: 8px; text-align: center; }
        .room img { max-width: 400px; border: 1px solid #ccc; }
        .room h3 { margin: 10px 0; color: #555; }
        .info { color: #666; font-size: 14px; }
    </style>
</head>
<body>
    <h1>🏨 Images des Chambres d'Hôtels</h1>

    <div class="hotel">
        <h2>Luxe Opéra - Montpellier (5★)</h2>
        <div class="rooms">
            <div class="room">
                <h3>Chambre 201</h3>
                <img src="http://localhost:8084/images/opera-room-201.svg" alt="Chambre 201">
                <p class="info">Suite Premium • 2 lits • 220€/nuit</p>
            </div>
            <div class="room">
                <h3>Chambre 202</h3>
                <img src="http://localhost:8084/images/opera-room-202.svg" alt="Chambre 202">
                <p class="info">Suite Deluxe • 2 lits • 240€/nuit</p>
            </div>
        </div>
    </div>

    <div class="hotel">
        <h2>Hôtel Rivage - Sète (4★)</h2>
        <div class="rooms">
            <div class="room">
                <h3>Chambre 101</h3>
                <img src="http://localhost:8082/images/rivage-room-101.svg" alt="Chambre 101">
                <p class="info">Vue Mer • 2 lits • 120€/nuit</p>
            </div>
            <div class="room">
                <h3>Chambre 102</h3>
                <img src="http://localhost:8082/images/rivage-room-102.svg" alt="Chambre 102">
                <p class="info">Familiale • 3 lits • 150€/nuit</p>
            </div>
        </div>
    </div>
</body>
</html>
HTMLEOF

echo -e "${GREEN}✓${NC} Page HTML générée : /tmp/test_images_chambres.html"
echo ""

echo "=============================================="
echo "  RÉSUMÉ"
echo "=============================================="
echo ""
echo "Images disponibles :"
echo "  • Opera 201 : http://localhost:8084/images/opera-room-201.svg"
echo "  • Opera 202 : http://localhost:8084/images/opera-room-202.svg"
echo "  • Rivage 101 : http://localhost:8082/images/rivage-room-101.svg"
echo "  • Rivage 102 : http://localhost:8082/images/rivage-room-102.svg"
echo ""
echo "Pour visualiser les images :"
echo "  firefox /tmp/test_images_chambres.html"
echo "  # ou"
echo "  xdg-open /tmp/test_images_chambres.html"
echo ""
echo "=============================================="

