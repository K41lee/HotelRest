package org.examples.agency.rest;

import dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

/**
 * Client REST pour communiquer avec les serveurs d'hôtels
 * Remplace les appels SOAP/WSDL
 */
@Component
public class HotelRestClient {

    private static final Logger log = LoggerFactory.getLogger(HotelRestClient.class);

    private final RestTemplate restTemplate;

    // Configuration des partenaires (hôtels)
    private static class Partner {
        final String code;
        final String baseUrl;
        final String defaultAgency;

        Partner(String code, String baseUrl, String defaultAgency) {
            this.code = code;
            this.baseUrl = baseUrl;
            this.defaultAgency = defaultAgency;
        }
    }

    private final List<Partner> partners = Arrays.asList(
        new Partner("rivage", "http://localhost:8082/api", "rivageAgency"),  // REST ✅
        new Partner("opera", "http://localhost:8084/api", "operaAgency")      // REST ✅
    );

    public HotelRestClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Test de connectivité avec un hôtel
     */
    public boolean ping(String hotelCode) {
        try {
            Partner partner = findPartner(hotelCode);
            if (partner == null) return false;

            String url = partner.baseUrl + "/ping";
            String response = restTemplate.getForObject(url, String.class);
            return "pong".equals(response);
        } catch (Exception e) {
            log.warn("[REST-CLIENT] ping {} failed: {}", hotelCode, e.getMessage());
            return false;
        }
    }

    /**
     * Obtenir le catalogue d'un hôtel
     */
    public CatalogDTO getCatalog(String hotelCode) {
        try {
            Partner partner = findPartner(hotelCode);
            if (partner == null) {
                log.warn("[REST-CLIENT] Unknown hotel code: {}", hotelCode);
                return null;
            }

            String url = partner.baseUrl + "/hotels/catalog";
            log.info("[AGENCY->HOTEL:{}] GET {}", partner.code, url);

            CatalogDTO catalog = restTemplate.getForObject(url, CatalogDTO.class);

            if (catalog != null) {
                log.info("[HOTEL:{}->AGENCY] Catalog: name={}, cities={}, agencies={}",
                        partner.code, catalog.getName(),
                        catalog.getCities() != null ? catalog.getCities().size() : 0,
                        catalog.getAgencies() != null ? catalog.getAgencies().size() : 0);
            }

            return catalog;
        } catch (Exception e) {
            log.warn("[REST-CLIENT] getCatalog {} failed: {}", hotelCode, e.getMessage());
            return null;
        }
    }

    /**
     * Rechercher des offres d'hôtel
     */
    public List<OfferDTO> searchOffers(String hotelCode, String ville, LocalDate arrivee,
                                       LocalDate depart, int nbPersonnes, String agencyId) {
        try {
            Partner partner = findPartner(hotelCode);
            if (partner == null) {
                log.warn("[REST-CLIENT] Unknown hotel code: {}", hotelCode);
                return Collections.emptyList();
            }

            // Déterminer l'agence effective
            String effectiveAgency = (agencyId == null || agencyId.trim().isEmpty())
                ? partner.defaultAgency
                : agencyId;

            // Construire l'URL avec query params
            String url = String.format(
                "%s/hotels/search?ville=%s&arrivee=%s&depart=%s&nbPersonnes=%d&agence=%s",
                partner.baseUrl,
                ville != null ? ville : "",
                arrivee != null ? arrivee.toString() : "",
                depart != null ? depart.toString() : "",
                nbPersonnes,
                effectiveAgency != null ? effectiveAgency : ""
            );

            log.info("[AGENCY->HOTEL:{}] GET {} (agence='{}')",
                    partner.code, url, effectiveAgency);

            // Appel REST
            OfferListDTO response = restTemplate.getForObject(url, OfferListDTO.class);

            List<OfferDTO> offers = response != null && response.getOffers() != null
                ? response.getOffers()
                : Collections.emptyList();

            log.info("[HOTEL:{}->AGENCY] Received {} offers", partner.code, offers.size());

            return offers;

        } catch (Exception e) {
            log.warn("[REST-CLIENT] searchOffers {} failed: {}", hotelCode, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Créer une réservation
     */
    public ReservationConfirmationDTO makeReservation(String hotelCode, String offerId,
                                                      String agencyId, String nom,
                                                      String prenom, String carte) {
        try {
            Partner partner = findPartner(hotelCode);
            if (partner == null) {
                log.warn("[REST-CLIENT] Unknown hotel code: {}", hotelCode);
                return ReservationConfirmationDTO.failure("Code hôtel inconnu");
            }

            // Déterminer l'agence effective
            String effectiveAgency = (agencyId == null || agencyId.trim().isEmpty())
                ? partner.defaultAgency
                : agencyId;

            // Construire la requête
            ReservationRequestDTO request = new ReservationRequestDTO();
            request.setOfferId(offerId);
            request.setAgence(effectiveAgency);
            request.setNom(nom);
            request.setPrenom(prenom);
            request.setCarte(carte);

            String url = partner.baseUrl + "/reservations";

            log.info("[AGENCY->HOTEL:{}] POST {} (offerId='{}', agence='{}', nom='{}', prenom='{}')",
                    partner.code, url, offerId, effectiveAgency, nom, prenom);

            // Appel REST avec POST
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ReservationRequestDTO> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ReservationConfirmationDTO> response = restTemplate.postForEntity(
                url, entity, ReservationConfirmationDTO.class);

            ReservationConfirmationDTO confirmation = response.getBody();

            if (confirmation != null) {
                log.info("[HOTEL:{}->AGENCY] Reservation: success={}, ref={}, message={}",
                        partner.code, confirmation.isSuccess(),
                        confirmation.getId(), confirmation.getMessage());
            }

            return confirmation != null ? confirmation : ReservationConfirmationDTO.failure("Pas de réponse");

        } catch (Exception e) {
            log.error("[REST-CLIENT] makeReservation {} failed: {}", hotelCode, e.getMessage());
            return ReservationConfirmationDTO.failure("Erreur: " + e.getMessage());
        }
    }

    /**
     * Obtenir tous les catalogues (tous les hôtels)
     */
    public List<CatalogDTO> getAllCatalogs() {
        List<CatalogDTO> catalogs = new ArrayList<>();
        for (Partner partner : partners) {
            CatalogDTO catalog = getCatalog(partner.code);
            if (catalog != null) {
                catalogs.add(catalog);
            }
        }
        return catalogs;
    }

    /**
     * Rechercher des offres sur tous les hôtels
     */
    public Map<String, List<OfferDTO>> searchAllOffers(String ville, LocalDate arrivee,
                                                        LocalDate depart, int nbPersonnes,
                                                        String agencyId) {
        Map<String, List<OfferDTO>> allOffers = new LinkedHashMap<>();

        for (Partner partner : partners) {
            List<OfferDTO> offers = searchOffers(
                partner.code, ville, arrivee, depart, nbPersonnes, agencyId);

            if (!offers.isEmpty()) {
                allOffers.put(partner.code, offers);
            }
        }

        return allOffers;
    }

    /**
     * Obtenir la liste des codes d'hôtels partenaires
     */
    public List<String> getPartnerCodes() {
        List<String> codes = new ArrayList<>();
        for (Partner partner : partners) {
            codes.add(partner.code);
        }
        return codes;
    }

    // Méthodes utilitaires

    private Partner findPartner(String code) {
        if (code == null) return null;
        return partners.stream()
            .filter(p -> p.code.equalsIgnoreCase(code))
            .findFirst()
            .orElse(null);
    }
}

