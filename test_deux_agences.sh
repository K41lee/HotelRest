#!/bin/bash

# Script de test des deux agences

echo "=========================================="
echo "  TEST DES DEUX AGENCES"
echo "=========================================="
echo ""

# Couleurs
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test Agence 1
echo -e "${BLUE}>>> TEST AGENCE 1 (MegaAgence - 10%)${NC}"
echo -e "${YELLOW}Port: 7070${NC}"
echo ""

echo "1. Test ping agence 1..."
RESULT1=$(echo '{"op":"ping"}' | nc -w 2 localhost 7070 2>/dev/null)
if echo "$RESULT1" | grep -q "pong"; then
    echo -e "${GREEN}✅ Agence 1 répond${NC}"
else
    echo "❌ Agence 1 ne répond pas"
fi
echo ""

echo "2. Test catalogue agence 1..."
CATALOG1=$(echo '{"op":"catalog.get"}' | nc -w 2 localhost 7070 2>/dev/null)
echo "$CATALOG1" | python3 -m json.tool 2>/dev/null | grep -E "name|discount" | head -2
echo ""

echo "3. Test recherche agence 1..."
SEARCH1=$(echo '{"op":"offers.search","payload":{"ville":"Montpellier","arrivee":"2025-12-25","depart":"2025-12-27","nbPersonnes":2}}' | nc -w 3 localhost 7070 2>/dev/null)
PRICE1=$(echo "$SEARCH1" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    offers = data.get('data', {}).get('offers', [])
    if offers:
        o = offers[0]
        print(f\"Prix original: {o.get('prixOriginal')}€ → Prix agence 1 (10%): {o.get('prixTotal')}€\")
    else:
        print('Aucune offre')
except:
    print('Erreur parsing')
" 2>/dev/null)
echo "$PRICE1"
echo ""

echo "=========================================="
echo ""

# Test Agence 2
echo -e "${BLUE}>>> TEST AGENCE 2 (SuperAgence - 20%)${NC}"
echo -e "${YELLOW}Port: 7071${NC}"
echo ""

echo "1. Test ping agence 2..."
RESULT2=$(echo '{"op":"ping"}' | nc -w 2 localhost 7071 2>/dev/null)
if echo "$RESULT2" | grep -q "pong"; then
    echo -e "${GREEN}✅ Agence 2 répond${NC}"
else
    echo "❌ Agence 2 ne répond pas"
fi
echo ""

echo "2. Test catalogue agence 2..."
CATALOG2=$(echo '{"op":"catalog.get"}' | nc -w 2 localhost 7071 2>/dev/null)
echo "$CATALOG2" | python3 -m json.tool 2>/dev/null | grep -E "name|discount" | head -2
echo ""

echo "3. Test recherche agence 2..."
SEARCH2=$(echo '{"op":"offers.search","payload":{"ville":"Montpellier","arrivee":"2025-12-25","depart":"2025-12-27","nbPersonnes":2}}' | nc -w 3 localhost 7071 2>/dev/null)
PRICE2=$(echo "$SEARCH2" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    offers = data.get('data', {}).get('offers', [])
    if offers:
        o = offers[0]
        print(f\"Prix original: {o.get('prixOriginal')}€ → Prix agence 2 (20%): {o.get('prixTotal')}€\")
    else:
        print('Aucune offre')
except:
    print('Erreur parsing')
" 2>/dev/null)
echo "$PRICE2"
echo ""

echo "=========================================="
echo -e "${GREEN}>>> COMPARAISON${NC}"
echo "=========================================="
echo ""
echo "Agence 1 (MegaAgence)  : 10% de réduction"
echo "Agence 2 (SuperAgence) : 20% de réduction"
echo ""
echo "Exemple pour une chambre à 440€:"
echo "  - Via Agence 1: 396€ (économie 44€)"
echo "  - Via Agence 2: 352€ (économie 88€) ⭐"
echo ""
echo "SuperAgence offre 44€ d'économie supplémentaire!"
echo ""
echo "=========================================="

