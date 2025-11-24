#!/bin/bash
# Script de diagnostic pour le problème d'images non disponibles

echo "=============================================="
echo "  DIAGNOSTIC ACCÈS AUX IMAGES"
echo "=============================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Fonction pour vérifier et afficher l'état d'un serveur
check_server() {
    local name=$1
    local port=$2
    local ping_url="http://localhost:$port/api/ping"

    echo -n "  Serveur $name (port $port): "

    RESPONSE=$(curl -s --connect-timeout 2 "$ping_url" 2>/dev/null)

    if [ "$RESPONSE" = "pong" ]; then
        echo -e "${GREEN}✓ Actif${NC}"
        return 0
    else
        echo -e "${RED}✗ Inactif${NC}"
        return 1
    fi
}

echo -e "${BLUE}[ÉTAPE 1/5]${NC} Vérification des serveurs"
echo ""

RIVAGE_OK=0
OPERA_OK=0

check_server "Rivage" 8082 && RIVAGE_OK=1
check_server "Opera" 8084 && OPERA_OK=1

if [ $RIVAGE_OK -eq 0 ] || [ $OPERA_OK -eq 0 ]; then
    echo ""
    echo -e "${YELLOW}⚠ Des serveurs ne sont pas actifs !${NC}"
    echo ""
    read -p "Voulez-vous démarrer les serveurs ? (o/N) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Oo]$ ]]; then
        echo "Démarrage des serveurs..."
        cd "$(dirname "$0")"
        ./lancement.sh --no-client > /tmp/lancement_diag.log 2>&1 &
        echo "Attente du démarrage (30 secondes)..."
        sleep 30

        # Revérifier
        check_server "Rivage" 8082 && RIVAGE_OK=1
        check_server "Opera" 8084 && OPERA_OK=1

        if [ $RIVAGE_OK -eq 0 ] || [ $OPERA_OK=0 ]; then
            echo -e "${RED}✗ Échec du démarrage${NC}"
            echo "Consultez les logs : tail -f logs/rivage.log logs/opera.log"
            exit 1
        fi
    else
        echo "Démarrez les serveurs manuellement : ./lancement.sh --no-client"
        exit 1
    fi
fi

echo ""
echo -e "${BLUE}[ÉTAPE 2/5]${NC} Vérification des fichiers images"
echo ""

FILES_OK=0

for file in \
    "server-opera/src/main/resources/static/images/opera-room-201.svg" \
    "server-opera/src/main/resources/static/images/opera-room-202.svg" \
    "server-rivage/src/main/resources/static/images/rivage-room-101.svg" \
    "server-rivage/src/main/resources/static/images/rivage-room-102.svg"
do
    if [ -f "$file" ]; then
        echo -e "  ${GREEN}✓${NC} $(basename $file)"
        ((FILES_OK++))
    else
        echo -e "  ${RED}✗${NC} $(basename $file) - MANQUANT"
    fi
done

if [ $FILES_OK -ne 4 ]; then
    echo -e "${RED}✗ Certains fichiers sont manquants !${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}[ÉTAPE 3/5]${NC} Test d'accès HTTP direct aux images"
echo ""

HTTP_OK=0

declare -A IMAGES=(
    ["Opera 201"]="http://localhost:8084/images/opera-room-201.svg"
    ["Opera 202"]="http://localhost:8084/images/opera-room-202.svg"
    ["Rivage 101"]="http://localhost:8082/images/rivage-room-101.svg"
    ["Rivage 102"]="http://localhost:8082/images/rivage-room-102.svg"
)

for name in "${!IMAGES[@]}"; do
    url="${IMAGES[$name]}"
    http_code=$(curl -s -o /dev/null -w "%{http_code}" "$url")

    echo -n "  $name: "
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}HTTP $http_code ✓${NC}"
        ((HTTP_OK++))
    else
        echo -e "${RED}HTTP $http_code ✗${NC}"
        echo "    URL testée: $url"
    fi
done

if [ $HTTP_OK -ne 4 ]; then
    echo ""
    echo -e "${RED}✗ Certaines images ne sont pas accessibles via HTTP !${NC}"
    echo ""
    echo "Causes possibles :"
    echo "  1. Les serveurs ne servent pas les fichiers static correctement"
    echo "  2. Le chemin /images/ n'est pas configuré"
    echo "  3. Les fichiers ne sont pas dans le classpath"
    echo ""
    echo "Vérification des logs des serveurs..."
    echo ""
    echo "=== Logs Rivage (dernières 10 lignes) ==="
    tail -10 logs/rivage.log
    echo ""
    echo "=== Logs Opera (dernières 10 lignes) ==="
    tail -10 logs/opera.log
    exit 1
fi

echo ""
echo -e "${BLUE}[ÉTAPE 4/5]${NC} Vérification de l'API REST (imageUrl dans la réponse)"
echo ""

API_OK=0

# Test Opera
SEARCH_OPERA=$(curl -s "http://localhost:8084/api/hotels/search?ville=Montpellier&arrivee=2025-12-25&depart=2025-12-27&nbPersonnes=2")
IMAGE_201=$(echo "$SEARCH_OPERA" | python3 -c "
import sys, json
try:
    offers = json.load(sys.stdin).get('offers', [])
    for o in offers:
        room = o.get('room', {})
        if room.get('numero') == 201:
            print(room.get('imageUrl', ''))
            break
except: pass
" 2>/dev/null)

echo -n "  Opera chambre 201: "
if [ -n "$IMAGE_201" ]; then
    echo -e "${GREEN}$IMAGE_201 ✓${NC}"
    ((API_OK++))
else
    echo -e "${RED}imageUrl manquant ✗${NC}"
    echo "    Réponse API: $(echo "$SEARCH_OPERA" | python3 -m json.tool 2>/dev/null | head -20)"
fi

# Test Rivage
SEARCH_RIVAGE=$(curl -s "http://localhost:8082/api/hotels/search?ville=Sete&arrivee=2025-12-25&depart=2025-12-27&nbPersonnes=2")
IMAGE_101=$(echo "$SEARCH_RIVAGE" | python3 -c "
import sys, json
try:
    offers = json.load(sys.stdin).get('offers', [])
    for o in offers:
        room = o.get('room', {})
        if room.get('numero') == 101:
            print(room.get('imageUrl', ''))
            break
except: pass
" 2>/dev/null)

echo -n "  Rivage chambre 101: "
if [ -n "$IMAGE_101" ]; then
    echo -e "${GREEN}$IMAGE_101 ✓${NC}"
    ((API_OK++))
else
    echo -e "${RED}imageUrl manquant ✗${NC}"
fi

if [ $API_OK -ne 2 ]; then
    echo ""
    echo -e "${RED}✗ L'API ne retourne pas les imageUrl !${NC}"
    echo ""
    echo "Problème : Les images ne sont pas dans les réponses de l'API REST"
    echo "Vérifiez que HotelRestServiceImpl.java inclut bien room.setImageUrl()"
    exit 1
fi

echo ""
echo -e "${BLUE}[ÉTAPE 5/5]${NC} Test du client (simulation de ce qu'il fait)"
echo ""

echo "Le client construit les URLs comme suit :"
echo ""

# Simuler ce que fait le client
OFFER_JSON='{"hotelName":"Luxe Opéra","room":{"numero":201,"imageUrl":"/images/opera-room-201.svg"}}'

HOTEL_NAME=$(echo "$OFFER_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin).get('hotelName',''))")
IMAGE_URL=$(echo "$OFFER_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin).get('room',{}).get('imageUrl',''))")

# Déterminer le port
if [[ "$HOTEL_NAME" =~ [Oo]pera ]]; then
    PORT=8084
else
    PORT=8082
fi

FULL_URL="http://localhost:$PORT$IMAGE_URL"

echo "  Hôtel détecté : $HOTEL_NAME"
echo "  Port déterminé : $PORT"
echo "  imageUrl reçu : $IMAGE_URL"
echo "  URL complète : $FULL_URL"
echo ""

HTTP_TEST=$(curl -s -o /dev/null -w "%{http_code}" "$FULL_URL")
echo -n "  Test d'accès : "
if [ "$HTTP_TEST" = "200" ]; then
    echo -e "${GREEN}HTTP $HTTP_TEST ✓${NC}"
else
    echo -e "${RED}HTTP $HTTP_TEST ✗${NC}"
fi

echo ""
echo "=============================================="
echo "  RÉSUMÉ DU DIAGNOSTIC"
echo "=============================================="
echo ""

TOTAL_ISSUES=0

if [ $RIVAGE_OK -eq 1 ] && [ $OPERA_OK -eq 1 ]; then
    echo -e "✓ Serveurs actifs : ${GREEN}OK${NC}"
else
    echo -e "✗ Serveurs actifs : ${RED}PROBLÈME${NC}"
    ((TOTAL_ISSUES++))
fi

if [ $FILES_OK -eq 4 ]; then
    echo -e "✓ Fichiers images : ${GREEN}OK (4/4)${NC}"
else
    echo -e "✗ Fichiers images : ${RED}PROBLÈME ($FILES_OK/4)${NC}"
    ((TOTAL_ISSUES++))
fi

if [ $HTTP_OK -eq 4 ]; then
    echo -e "✓ Accès HTTP : ${GREEN}OK (4/4)${NC}"
else
    echo -e "✗ Accès HTTP : ${RED}PROBLÈME ($HTTP_OK/4)${NC}"
    ((TOTAL_ISSUES++))
fi

if [ $API_OK -eq 2 ]; then
    echo -e "✓ API REST : ${GREEN}OK (imageUrl présent)${NC}"
else
    echo -e "✗ API REST : ${RED}PROBLÈME ($API_OK/2)${NC}"
    ((TOTAL_ISSUES++))
fi

echo ""

if [ $TOTAL_ISSUES -eq 0 ]; then
    echo -e "${GREEN}🎉 Tout fonctionne correctement !${NC}"
    echo ""
    echo "Le client devrait pouvoir accéder aux images."
    echo "Si le problème persiste :"
    echo "  1. Vérifiez les logs du client"
    echo "  2. Ajoutez des System.out.println() dans ResultsPanel.java"
    echo "  3. Vérifiez que le client lit bien room.imageUrl"
else
    echo -e "${RED}✗ $TOTAL_ISSUES problème(s) détecté(s)${NC}"
    echo ""
    echo "Corrigez les problèmes ci-dessus avant de tester le client."
fi

echo ""
echo "=============================================="

