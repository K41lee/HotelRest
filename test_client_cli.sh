#!/bin/bash
# Script de test du client CLI en mode automatique

echo "========================================="
echo "  TEST CLIENT CLI - Mode Agence REST"
echo "========================================="
echo ""

# Vérifier que l'agence est disponible
echo "1. Test de connectivité à l'agence (port 7070)..."
if echo '{"op":"ping"}' | nc -w 2 localhost 7070 | grep -q "pong"; then
    echo "   ✓ Agence disponible"
else
    echo "   ✗ Agence non disponible. Démarrez-la avec:"
    echo "     cd agency-server && mvn spring-boot:run"
    exit 1
fi

echo ""
echo "2. Catalogue via l'agence..."
CATALOG=$(echo '{"op":"catalog.get"}' | nc -w 2 localhost 7070)
echo "$CATALOG" | python3 -m json.tool 2>/dev/null || echo "$CATALOG"

echo ""
echo "3. Recherche d'offres via l'agence..."
OFFERS=$(echo '{"op":"offers.search","payload":{"ville":"Montpellier","arrivee":"2025-12-15","depart":"2025-12-17","nbPersonnes":2}}' | nc -w 2 localhost 7070)
echo "$OFFERS" | python3 -m json.tool 2>/dev/null | head -40

OFFER_COUNT=$(echo "$OFFERS" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('data',{}).get('offers',[])))" 2>/dev/null || echo "0")
echo ""
echo "   → $OFFER_COUNT offres trouvées"

if [ "$OFFER_COUNT" -gt 0 ]; then
    echo ""
    echo "4. Client CLI prêt à être utilisé !"
    echo ""
    echo "   Pour lancer le client CLI interactif :"
    echo "   cd client-cli"
    echo "   mvn exec:java -Dexec.mainClass=org.examples.client.ClientMain"
    echo ""
    echo "   Pour lancer le client GUI :"
    echo "   mvn exec:java -Dexec.mainClass=org.examples.client.gui.HotelClientGUI"
else
    echo ""
    echo "   ⚠ Aucune offre trouvée. Vérifiez que les serveurs d'hôtels sont démarrés."
fi

echo ""
echo "========================================="
echo "  FIN DES TESTS"
echo "========================================="

