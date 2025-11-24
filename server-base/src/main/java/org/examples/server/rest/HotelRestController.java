package org.examples.server.rest;

import dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Contrôleur REST pour les services hôteliers
 * Remplace l'ancien endpoint SOAP
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HotelRestController {
    
    private static final Logger logger = LoggerFactory.getLogger(HotelRestController.class);
    
    @Autowired(required = false)
    private HotelRestService hotelService;
    
    /**
     * Health check endpoint
     * GET /api/ping
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        logger.info("[REST] ping received");
        return ResponseEntity.ok("pong");
    }
    
    /**
     * Obtenir le catalogue de l'hôtel
     * GET /api/hotels/catalog
     */
    @GetMapping("/hotels/catalog")
    public ResponseEntity<?> getCatalog() {
        logger.info("[REST] GET /api/hotels/catalog");
        try {
            if (hotelService == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorDTO(503, "Service Unavailable", "Hotel service not available"));
            }
            
            CatalogDTO catalog = hotelService.getCatalog();
            return ResponseEntity.ok(catalog);
        } catch (Exception e) {
            logger.error("[REST] Error in getCatalog", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDTO(500, "Internal Server Error", e.getMessage()));
        }
    }
    
    /**
     * Rechercher des offres d'hôtel
     * GET /api/hotels/search?ville=Paris&arrivee=2025-12-01&depart=2025-12-03&nbPersonnes=2
     */
    @GetMapping("/hotels/search")
    public ResponseEntity<?> searchOffers(
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String arrivee,
            @RequestParam(required = false) String depart,
            @RequestParam(required = false) Integer prixMin,
            @RequestParam(required = false) Integer prixMax,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) Integer nbEtoiles,
            @RequestParam(defaultValue = "1") int nbPersonnes,
            @RequestParam(required = false) String agence) {
        
        logger.info("[REST] GET /api/hotels/search ville={} arrivee={} depart={} nbPersonnes={} agence={}", 
                    ville, arrivee, depart, nbPersonnes, agence);
        
        try {
            if (hotelService == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorDTO(503, "Service Unavailable", "Hotel service not available"));
            }
            
            // Construire le DTO de recherche
            SearchRequestDTO searchRequest = new SearchRequestDTO();
            searchRequest.setVille(ville);
            searchRequest.setNbPersonnes(nbPersonnes);
            searchRequest.setPrixMin(prixMin);
            searchRequest.setPrixMax(prixMax);
            searchRequest.setCategorie(categorie);
            searchRequest.setNbEtoiles(nbEtoiles);
            searchRequest.setAgence(agence);
            
            // Parser les dates
            if (arrivee != null && !arrivee.trim().isEmpty()) {
                searchRequest.setArrivee(java.time.LocalDate.parse(arrivee));
            }
            if (depart != null && !depart.trim().isEmpty()) {
                searchRequest.setDepart(java.time.LocalDate.parse(depart));
            }
            
            List<OfferDTO> offers = hotelService.searchOffers(searchRequest);
            return ResponseEntity.ok(new OfferListDTO(offers));
            
        } catch (java.time.format.DateTimeParseException e) {
            logger.warn("[REST] Invalid date format", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorDTO(400, "Bad Request", "Format de date invalide (attendu: yyyy-MM-dd)"));
        } catch (Exception e) {
            logger.error("[REST] Error in searchOffers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDTO(500, "Internal Server Error", e.getMessage()));
        }
    }
    
    /**
     * Rechercher des offres (POST avec body JSON)
     * POST /api/hotels/search
     */
    @PostMapping("/hotels/search")
    public ResponseEntity<?> searchOffersPost(@RequestBody SearchRequestDTO searchRequest) {
        logger.info("[REST] POST /api/hotels/search {}", searchRequest);
        
        try {
            if (hotelService == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorDTO(503, "Service Unavailable", "Hotel service not available"));
            }
            
            List<OfferDTO> offers = hotelService.searchOffers(searchRequest);
            return ResponseEntity.ok(new OfferListDTO(offers));
            
        } catch (Exception e) {
            logger.error("[REST] Error in searchOffersPost", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDTO(500, "Internal Server Error", e.getMessage()));
        }
    }
    
    /**
     * Créer une réservation
     * POST /api/reservations
     */
    @PostMapping("/reservations")
    public ResponseEntity<?> makeReservation(@RequestBody ReservationRequestDTO request) {
        logger.info("[REST] POST /api/reservations {}", request);
        
        try {
            if (hotelService == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorDTO(503, "Service Unavailable", "Hotel service not available"));
            }
            
            ReservationConfirmationDTO confirmation = hotelService.makeReservation(request);
            
            if (confirmation.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(confirmation);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(confirmation);
            }
            
        } catch (Exception e) {
            logger.error("[REST] Error in makeReservation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorDTO(500, "Internal Server Error", e.getMessage()));
        }
    }
}

