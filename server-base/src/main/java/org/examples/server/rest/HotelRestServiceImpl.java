package org.examples.server.rest;

import dto.*;
import Impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.text.Normalizer;

/**
 * Implémentation REST du service hôtelier
 * Réutilise la logique métier de Gestionnaire
 */
@Service
public class HotelRestServiceImpl implements HotelRestService {
    
    private static final Logger logger = LoggerFactory.getLogger(HotelRestServiceImpl.class);
    
    @Autowired(required = false)
    private Gestionnaire gestionnaire;
    
    @Autowired(required = false)
    private DataFactory factory;
    
    @Autowired(required = false)
    private org.examples.server.service.HotelDatabaseService dbService;
    
    // Suivi local des réservations (cache mémoire)
    private final Map<String, List<ReservationPeriod>> reservationsByHotel = new ConcurrentHashMap<>();
    
    private static class ReservationPeriod {
        final int roomNumber;
        final LocalDate from;
        final LocalDate to;
        
        ReservationPeriod(int roomNumber, LocalDate from, LocalDate to) {
            this.roomNumber = roomNumber;
            this.from = from;
            this.to = to;
        }
        
        boolean overlaps(LocalDate f, LocalDate t) {
            return (from.isBefore(t) && f.isBefore(to));
        }
    }
    
    @Override
    public CatalogDTO getCatalog() {
        logger.info("[REST-SERVICE] getCatalog");
        
        CatalogDTO catalog = new CatalogDTO();
        
        if (gestionnaire != null && !gestionnaire.getHotels().isEmpty()) {
            Hotel h = gestionnaire.getHotels().get(0);
            catalog.setName(h.getNom());
            
            // Ajouter les villes
            List<String> cities = gestionnaire.getHotels().stream()
                .map(Hotel::getAdresse)
                .filter(Objects::nonNull)
                .map(Adresse::getVille)
                .distinct()
                .collect(Collectors.toList());
            catalog.setCities(cities);
            
            // Ajouter les agences
            List<String> agencies = h.getAgences().stream()
                .map(Agence::getNom)
                .collect(Collectors.toList());
            catalog.setAgencies(agencies);
            
            logger.info("[REST-SERVICE] Catalog: name={} cities={} agencies={}", 
                       catalog.getName(), cities.size(), agencies.size());
        } else if (factory != null) {
            catalog.setName(factory.getHotelName());
            logger.info("[REST-SERVICE] Catalog (fallback): name={}", catalog.getName());
        } else {
            catalog.setName("Unknown Hotel");
            logger.warn("[REST-SERVICE] No gestionnaire or factory available");
        }
        
        return catalog;
    }
    
    @Override
    public List<OfferDTO> searchOffers(SearchRequestDTO criteria) {
        logger.info("[REST-SERVICE] searchOffers: ville={} arrivee={} depart={} nbPersonnes={} agence={}",
                   criteria.getVille(), criteria.getArrivee(), criteria.getDepart(), 
                   criteria.getNbPersonnes(), criteria.getAgence());
        
        if (gestionnaire == null) {
            logger.warn("[REST-SERVICE] No gestionnaire available");
            return Collections.emptyList();
        }
        
        // Valider les dates
        LocalDate from = criteria.getArrivee() != null ? criteria.getArrivee() : LocalDate.now();
        LocalDate to = criteria.getDepart() != null ? criteria.getDepart() : from.plusDays(1);
        
        if (!from.isBefore(to)) {
            logger.warn("[REST-SERVICE] Invalid date range: from={} to={}", from, to);
            return Collections.emptyList();
        }
        
        // Convertir la catégorie
        Categorie catEnum = null;
        if (criteria.getCategorie() != null && !criteria.getCategorie().trim().isEmpty()) {
            try {
                catEnum = Categorie.valueOf(criteria.getCategorie().trim());
            } catch (Exception e) {
                logger.info("[REST-SERVICE] Unknown category: {}", criteria.getCategorie());
            }
        }
        
        // Rechercher les offres via le gestionnaire
        List<Gestionnaire.Offre> matches = gestionnaire.findMatchReservation(
            safeString(criteria.getVille()),
            from,
            to,
            criteria.getPrixMin(),
            criteria.getPrixMax(),
            catEnum,
            criteria.getNbEtoiles(),
            criteria.getNbPersonnes(),
            criteria.getAgence()
        );
        
        // Fuzzy matching sur la ville si aucune offre trouvée
        if (matches.isEmpty() && criteria.getVille() != null && !criteria.getVille().trim().isEmpty()) {
            String normQuery = normalize(criteria.getVille());
            for (Hotel h : gestionnaire.getHotels()) {
                if (h.getAdresse() != null) {
                    String normHotelCity = normalize(h.getAdresse().getVille());
                    if (normHotelCity.startsWith(normQuery) || normQuery.startsWith(normHotelCity)) {
                        logger.info("[REST-SERVICE] Fuzzy match: {} -> {}", 
                                   criteria.getVille(), h.getAdresse().getVille());
                        matches = gestionnaire.findMatchReservation(
                            h.getAdresse().getVille(), from, to,
                            criteria.getPrixMin(), criteria.getPrixMax(),
                            catEnum, criteria.getNbEtoiles(),
                            criteria.getNbPersonnes(), criteria.getAgence()
                        );
                        break;
                    }
                }
            }
        }
        
        logger.info("[REST-SERVICE] Found {} raw offers", matches.size());
        
        // Convertir en DTOs
        List<OfferDTO> offers = new ArrayList<>();
        for (Gestionnaire.Offre o : matches) {
            Chambre c = o.chambre();
            Hotel h = o.hotel();
            
            if (c == null || h == null) continue;
            
            // Filtrer les chambres déjà réservées (cache local)
            if (!isRoomAvailableLocal(h.getNom(), c.getNumero(), from, to)) {
                logger.info("[REST-SERVICE] Room {} of '{}' filtered (already reserved)", 
                           c.getNumero(), h.getNom());
                continue;
            }
            
            OfferDTO offer = new OfferDTO();
            offer.setOfferId(buildOfferId(h, c, from, to, criteria.getNbPersonnes()));
            offer.setHotelName(h.getNom());
            offer.setNbLits(c.getNbLits());
            offer.setNbEtoiles(h.getNbEtoiles());
            offer.setCategorie(String.valueOf(h.getCategorie()));
            offer.setRoomNumber(c.getNumero());
            offer.setStart(from);
            offer.setEnd(to);
            offer.setAgenceApplied(criteria.getAgence());
            
            // Adresse
            if (h.getAdresse() != null) {
                AddressDTO address = new AddressDTO();
                address.setVille(h.getAdresse().getVille());
                address.setPays(h.getAdresse().getPays());
                address.setRue(h.getAdresse().getRue());
                address.setNumero(h.getAdresse().getNumero());
                address.setLieuDit(h.getAdresse().getLieuDit());
                double[] gps = h.getAdresse().getPositionGps();
                if (gps != null && gps.length >= 2) {
                    address.setLatitude(gps[0]);
                    address.setLongitude(gps[1]);
                }
                offer.setAddress(address);
            }
            
            // Chambre
            RoomDTO room = new RoomDTO();
            room.setNumero(c.getNumero());
            room.setNbLits(c.getNbLits());
            room.setPrixParNuit(c.getPrixParNuit());
            offer.setRoom(room);
            
            // Prix total
            int nights = (int) ChronoUnit.DAYS.between(from, to);
            int base = o.prixTotal() > 0 ? o.prixTotal() : (c.getPrixParNuit() * nights);
            offer.setPrixTotal(base);
            
            offers.add(offer);
            
            logger.info("[REST-SERVICE] Offer: id={} hotel='{}' room={} price={} nights={}",
                       offer.getOfferId(), offer.getHotelName(), offer.getRoomNumber(), 
                       offer.getPrixTotal(), nights);
        }
        
        logger.info("[REST-SERVICE] Returning {} offers", offers.size());
        return offers;
    }
    
    @Override
    public ReservationConfirmationDTO makeReservation(ReservationRequestDTO request) {
        logger.info("[REST-SERVICE] makeReservation: agence={} offerId={} hotel={} room={} nom={} prenom={}",
                   request.getAgence(), request.getOfferId(), request.getHotelName(), 
                   request.getRoomNumber(), request.getNom(), request.getPrenom());
        
        ReservationConfirmationDTO confirmation = new ReservationConfirmationDTO();
        
        if (gestionnaire == null) {
            confirmation.setSuccess(false);
            confirmation.setMessage("Service non disponible");
            return confirmation;
        }
        
        // Parser l'offerId ou utiliser les paramètres directs
        ParsedOfferId parsed = parseOfferId(request.getOfferId());
        Hotel hotel = null;
        Chambre chambre = null;
        LocalDate from = null;
        LocalDate to = null;
        
        if (parsed != null) {
            hotel = findHotelByKey(parsed.hotelKey);
            if (hotel != null) {
                chambre = findRoom(hotel, parsed.roomNumber);
            }
            from = parsed.from;
            to = parsed.to;
        } else if (request.getHotelName() != null && request.getRoomNumber() > 0 
                   && request.getArrivee() != null && request.getDepart() != null) {
            hotel = findHotelByName(request.getHotelName());
            if (hotel != null) {
                chambre = findRoom(hotel, request.getRoomNumber());
            }
            from = request.getArrivee();
            to = request.getDepart();
        }
        
        // Validation
        if (hotel == null || chambre == null || from == null || to == null) {
            confirmation.setSuccess(false);
            confirmation.setMessage("Paramètres invalides ou hôtel/chambre introuvable");
            return confirmation;
        }
        
        if (!from.isBefore(to)) {
            confirmation.setSuccess(false);
            confirmation.setMessage("Période invalide");
            return confirmation;
        }
        
        // Vérifier disponibilité (cache local)
        if (!isRoomAvailableLocal(hotel.getNom(), chambre.getNumero(), from, to)) {
            confirmation.setSuccess(false);
            confirmation.setMessage("Chambre déjà réservée (cache)");
            return confirmation;
        }
        
        // Vérifier disponibilité (gestionnaire)
        if (!chambre.isDisponible(from, to)) {
            confirmation.setSuccess(false);
            confirmation.setMessage("Chambre non disponible");
            return confirmation;
        }
        
        // Créer le client et effectuer la réservation
        Client client = new Client(request.getNom(), request.getPrenom(), request.getCarte());
        String ref = null;
        
        try {
            gestionnaire.makeReservation(client, chambre, from, to);
            registerReservation(hotel.getNom(), chambre.getNumero(), from, to);
            
            // Persister dans la base de données si disponible
            if (dbService != null) {
                try {
                    Optional<org.examples.server.entity.HotelEntity> hotelEntity = 
                        dbService.findHotelByNom(hotel.getNom());
                    
                    if (hotelEntity.isPresent()) {
                        List<org.examples.server.entity.ChambreEntity> chambres = 
                            dbService.findChambresByHotel(hotelEntity.get().getId());
                        
                        final int roomNum = chambre.getNumero();
                        org.examples.server.entity.ChambreEntity chambreEntity = chambres.stream()
                            .filter(c -> c.getNumero() == roomNum)
                            .findFirst()
                            .orElse(null);
                        
                        if (chambreEntity != null) {
                            org.examples.server.entity.ReservationEntity reservation = 
                                dbService.createReservation(chambreEntity, request.getNom(), 
                                                           request.getPrenom(), request.getCarte(),
                                                           request.getAgence(), from, to);
                            ref = reservation.getReference();
                            logger.info("[REST-SERVICE] Reservation persisted in DB: ref={}", ref);
                        }
                    }
                } catch (Exception dbEx) {
                    logger.warn("[REST-SERVICE] Failed to persist reservation: {}", dbEx.getMessage());
                }
            }
            
            // Générer une référence si pas de DB
            if (ref == null) {
                ref = hotel.getNom().substring(0, Math.min(4, hotel.getNom().length())).toUpperCase() 
                      + "-" + UUID.randomUUID();
            }
            
            confirmation.setSuccess(true);
            confirmation.setMessage("Réservation confirmée");
            confirmation.setId(ref);
            
            logger.info("[REST-SERVICE] Reservation OK: ref={} hotel='{}' room={} from={} to={}",
                       ref, hotel.getNom(), chambre.getNumero(), from, to);
            
        } catch (Exception e) {
            logger.error("[REST-SERVICE] Reservation error", e);
            confirmation.setSuccess(false);
            confirmation.setMessage("Erreur lors de la réservation: " + e.getMessage());
        }
        
        return confirmation;
    }
    
    // ========== Méthodes utilitaires ==========
    
    private void registerReservation(String hotelName, int roomNumber, LocalDate from, LocalDate to) {
        String key = normalize(hotelName);
        reservationsByHotel.computeIfAbsent(key, k -> new ArrayList<>())
            .add(new ReservationPeriod(roomNumber, from, to));
        logger.info("[REST-SERVICE] Reservation tracked: hotel='{}' room={} from={} to={}",
                   hotelName, roomNumber, from, to);
    }
    
    private boolean isRoomAvailableLocal(String hotelName, int roomNumber, LocalDate from, LocalDate to) {
        // 1. Vérifier le cache mémoire
        String key = normalize(hotelName);
        List<ReservationPeriod> list = reservationsByHotel.get(key);
        if (list != null) {
            for (ReservationPeriod rp : list) {
                if (rp.roomNumber == roomNumber && rp.overlaps(from, to)) {
                    logger.info("[REST-SERVICE] Room {} unavailable (cache): from={} to={}",
                               roomNumber, from, to);
                    return false;
                }
            }
        }

        // 2. Vérifier la base de données H2 si disponible
        if (dbService != null) {
            try {
                Optional<org.examples.server.entity.HotelEntity> hotelEntity =
                    dbService.findHotelByNom(hotelName);

                if (hotelEntity.isPresent()) {
                    List<org.examples.server.entity.ChambreEntity> chambres =
                        dbService.findChambresByHotel(hotelEntity.get().getId());

                    final int roomNum = roomNumber;
                    org.examples.server.entity.ChambreEntity chambreEntity = chambres.stream()
                        .filter(c -> c.getNumero() == roomNum)
                        .findFirst()
                        .orElse(null);

                    if (chambreEntity != null) {
                        List<org.examples.server.entity.ReservationEntity> overlapping =
                            dbService.findReservationsByChambre(chambreEntity.getId()).stream()
                            .filter(r -> r.getDebut().isBefore(to) && from.isBefore(r.getFin()))
                            .collect(Collectors.toList());

                        if (!overlapping.isEmpty()) {
                            logger.info("[REST-SERVICE] Room {} unavailable (DB): {} overlapping reservations from={} to={}",
                                       roomNumber, overlapping.size(), from, to);
                            return false;
                        }
                    }
                }
            } catch (Exception dbEx) {
                logger.warn("[REST-SERVICE] Failed to check DB for room availability: {}", dbEx.getMessage());
            }
        }

        return true;
    }
    
    private Hotel findHotelByKey(String hotelKey) {
        if (gestionnaire == null) return null;
        return gestionnaire.getHotels().stream()
            .filter(h -> normalize(h.getNom()).equals(hotelKey))
            .findFirst()
            .orElse(null);
    }
    
    private Hotel findHotelByName(String name) {
        if (gestionnaire == null) return null;
        return gestionnaire.getHotels().stream()
            .filter(h -> normalize(h.getNom()).equals(normalize(name)))
            .findFirst()
            .orElse(null);
    }
    
    private Chambre findRoom(Hotel hotel, int roomNumber) {
        return hotel.getChambres().stream()
            .filter(c -> c.getNumero() == roomNumber)
            .findFirst()
            .orElse(null);
    }
    
    private String buildOfferId(Hotel h, Chambre c, LocalDate from, LocalDate to, int persons) {
        return "OF|" + normalize(h.getNom()) + "|" + c.getNumero() + "|" + from + "|" + to + "|" + persons;
    }
    
    private static class ParsedOfferId {
        String hotelKey;
        int roomNumber;
        LocalDate from;
        LocalDate to;
        int persons;
    }
    
    private ParsedOfferId parseOfferId(String offerId) {
        if (offerId == null || !offerId.startsWith("OF|")) return null;
        String[] parts = offerId.split("\\|");
        if (parts.length != 6) return null;
        
        try {
            ParsedOfferId p = new ParsedOfferId();
            p.hotelKey = parts[1];
            p.roomNumber = Integer.parseInt(parts[2]);
            p.from = LocalDate.parse(parts[3]);
            p.to = LocalDate.parse(parts[4]);
            p.persons = Integer.parseInt(parts[5]);
            return p;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        n = n.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        return n;
    }
    
    private static String safeString(String s) {
        return s == null ? "" : s.trim();
    }
}

