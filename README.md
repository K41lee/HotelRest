# 🏨 Système de Réservation d'Hôtels - Multi-Agences REST

## Vue d'ensemble

Ce dépôt contient une application de réservation d'hôtels multi-agences basée sur des APIs REST/JSON, des bases H2 locales et un client GUI Swing pour la comparaison et la réservation.

## Contenu principal

- `server-opera/` : Service hôtel "Opera" (Spring Boot, REST, H2, images statiques)
- `server-rivage/` : Service hôtel "Rivage" (Spring Boot, REST, H2, images statiques)
- `server-base/` : Code commun pour les serveurs (controllers REST, services, entités JPA)
- `agency-server/` et `agency-server-2/` : Agences (TCP/JSON) qui consomment les APIs REST et appliquent des réductions
- `client-cli/` : Client GUI Swing (recherche multi-agence, tri, affichage d'images, réservation)
- `domain/` : DTOs partagés
- `lancement.sh` : Script principal de démarrage (démarre les serveurs, agences puis GUI)
- `reset_databases.sh` : Script de réinitialisation des bases H2

## Démarrage rapide

1. Compiler :

```bash
mvn clean install -DskipTests
```

2. Lancer tout :

```bash
./lancement.sh
```

Options :
- `--no-gui` : ne démarre pas l'interface graphique
- `--no-client` : ne démarre pas le client (utile pour tests serveurs)
- `--arret-propre` : arrête proprement tous les serveurs (serveurs hôteliers et agences) à la fin du processus GUI

## Endpoints REST (exemples)

- `GET /api/ping` — health check
- `GET /api/hotels/catalog` — catalogue
- `GET /api/hotels/search` — recherche par query params
- `POST /api/hotels/search` — recherche via JSON body
- `POST /api/reservations` — création de réservation
- `GET /images/{filename}` — images statiques (SVG)

## Images

Les images SVG doivent être placées dans `src/main/resources/static/images/` des serveurs (`server-opera`, `server-rivage`). Les DTOs peuvent inclure `room.imageUrl` (chemin relatif) et, si nécessaire, `room.imageData` (Base64 du contenu SVG). Pour le client Swing, il est recommandé d'utiliser une bibliothèque de rendu SVG (ex: Batik) ou de fournir une version PNG pré-rendue.

## Base H2

Fichiers de données H2 sont exclus du dépôt (par design) :
- `server-opera/data/`
- `server-rivage/data/`

Utilisez `./reset_databases.sh` pour recréer les données initiales si nécessaire.

## Nettoyage de la documentation

Les fichiers README locaux dans des sous-packages ont été consolidés dans ce `README.md`. Les fichiers `.md` non essentiels ont été supprimés pour éviter la duplication.

## Notes pour les développeurs

- Vérifier `IMAGE_URL` dans la table `chambres` si les images ne s'affichent pas.
- Si le client affiche "Impossible de décoder l'image", vérifier que `room.imageData` est bien un Base64 valide ou que le fichier SVG est accessible via `imageUrl`.
