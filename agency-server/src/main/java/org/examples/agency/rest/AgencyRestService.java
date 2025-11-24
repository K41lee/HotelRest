package org.examples.agency.rest;

import dto.*;
import org.examples.agency.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Service d'agence utilisant REST pour communiquer avec les hôtels
 * Remplace AgencyService (SOAP)
 */
@Service
public class AgencyRestService {
    
    private static final Logger log = LoggerFactory.getLogger(AgencyRestService.class);
    
    @Autowired
    private HotelRestClient hotelClient;
    
    /**
     * Gérer une requête JSON du client TCP
     */
    public String handleRequest(String jsonLine) {
        try {
            log.info("[AGENCY-REST-REQ] raw={}", jsonLine);
            Map<String, Object> req = Json.minParse(jsonLine);
            String op = (String) req.get("op");
            
            if ("ping".equals(op)) {
                return Json.ok(Collections.singletonMap("pong", true));
            }
            
            if ("catalog.get".equals(op)) {
                log.info("[AGENCY-REST] op=catalog.get");
                return Json.ok(getCatalog());
            }
            
            if ("offers.search".equals(op)) {
                log.info("[AGENCY-REST] op=offers.search payload={}", req.get("payload"));
                return Json.ok(searchOffers((Map<String, Object>) req.get("payload")));
            }
            
            if ("reservation.make".equals(op)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> p = (Map<String, Object>) req.get("payload");
                String masked = p != null && p.get("carte") != null 
                    ? maskCard(String.valueOf(p.get("carte"))) 
                    : null;
                    
                log.info("[AGENCY-REST] op=reservation.make payload={{hotelCode={}, offerId={}, agencyId={}, nom={}, prenom={}, carte={}}}",
                         p != null ? p.get("hotelCode") : null,
                         p != null ? p.get("offerId") : null,
                         p != null ? p.get("agencyId") : null,
                         p != null ? p.get("nom") : null,
                         p != null ? p.get("prenom") : null,
                         masked);
                         
                return Json.ok(makeReservation(p));
            }
            
            return Json.error("unknown op");
            
        } catch (Exception e) {
            log.warn("[AGENCY-REST] handle error: {}", e.toString());
            return Json.error(e.getMessage());
        }
    }
    
    /**
     * Obtenir le catalogue agrégé de tous les hôtels
     */
    private Map<String, Object> getCatalog() {
        Set<String> cities = new LinkedHashSet<>();
        Set<String> agencies = new LinkedHashSet<>();
        
        List<CatalogDTO> catalogs = hotelClient.getAllCatalogs();
        
        for (CatalogDTO catalog : catalogs) {
            if (catalog.getCities() != null) {
                cities.addAll(catalog.getCities());
            }
            if (catalog.getAgencies() != null) {
                agencies.addAll(catalog.getAgencies());
            }
        }
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "Agence Centrale");
        data.put("cities", new ArrayList<>(cities));
        data.put("agencies", new ArrayList<>(agencies));
        
        log.info("[AGENCY-REST] Catalog: cities={}, agencies={}", cities.size(), agencies.size());
        
        return data;
    }
    
    /**
     * Rechercher des offres sur tous les hôtels partenaires
     */
    private Map<String, Object> searchOffers(Map<String, Object> payload) throws Exception {
        String ville = str(payload.get("ville"));
        String arrivee = str(payload.get("arrivee"));
        String depart = str(payload.get("depart"));
        int nb = num(payload.get("nbPersonnes"), 1);
        String agencyId = str(payload.get("agencyId"));
        
        log.info("[AGENCY-REST] searchOffers ville='{}' arrivee='{}' depart='{}' nbPersonnes={} agencyId='{}'",
                ville, arrivee, depart, nb, agencyId);
        
        LocalDate from = LocalDate.parse(arrivee);
        LocalDate to = LocalDate.parse(depart);
        
        List<Map<String, Object>> offers = new ArrayList<>();
        
        // Rechercher sur tous les partenaires
        Map<String, List<OfferDTO>> allOffers = hotelClient.searchAllOffers(
            ville, from, to, nb, agencyId);
        
        // Convertir les DTOs en Maps pour le format JSON attendu
        for (Map.Entry<String, List<OfferDTO>> entry : allOffers.entrySet()) {
            String hotelCode = entry.getKey();
            List<OfferDTO> hotelOffers = entry.getValue();
            
            for (OfferDTO offer : hotelOffers) {
                Map<String, Object> m = new LinkedHashMap<>();
                
                m.put("hotelName", offer.getHotelName());
                m.put("categorie", offer.getCategorie());
                m.put("nbEtoiles", offer.getNbEtoiles());
                
                // Adresse
                if (offer.getAddress() != null) {
                    Map<String, Object> a = new LinkedHashMap<>();
                    a.put("pays", offer.getAddress().getPays());
                    a.put("ville", offer.getAddress().getVille());
                    a.put("rue", offer.getAddress().getRue());
                    a.put("numero", offer.getAddress().getNumero());
                    m.put("address", a);
                    m.put("city", offer.getAddress().getVille());
                    m.put("pays", offer.getAddress().getPays());
                    m.put("ville", offer.getAddress().getVille());
                    m.put("rue", offer.getAddress().getRue());
                    m.put("numero", offer.getAddress().getNumero());
                }
                
                // Chambre
                if (offer.getRoom() != null) {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("numero", offer.getRoom().getNumero());
                    r.put("nbLits", offer.getRoom().getNbLits());
                    m.put("room", r);
                }
                
                // Dates
                m.put("start", offer.getStart() != null ? offer.getStart().toString() : null);
                m.put("end", offer.getEnd() != null ? offer.getEnd().toString() : null);
                
                // Prix avec remise de 10% (commission agence)
                double originalPrice = offer.getPrixTotal();
                double discounted = Math.round(originalPrice * 0.9 * 100.0) / 100.0;
                m.put("prixOriginal", originalPrice);
                m.put("prixTotal", discounted);
                
                log.info("[AGENCY-REST] Applied 10% discount for partner {} offerId={}: {} -> {}",
                        hotelCode, offer.getOfferId(), originalPrice, discounted);
                
                m.put("agenceApplied", offer.getAgenceApplied());
                m.put("offerId", offer.getOfferId());
                m.put("hotelCode", hotelCode);
                
                // URL d'image
                if (offer.getRoom() != null && offer.getRoom().getImageUrl() != null) {
                    m.put("imageUrl", offer.getRoom().getImageUrl());
                }
                
                offers.add(m);
            }
            
            log.info("[AGENCY-REST] Added {} offers from hotel '{}'", hotelOffers.size(), hotelCode);
        }
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("offers", offers);
        
        log.info("[AGENCY-REST] Total offers: {}", offers.size());
        
        return data;
    }
    
    /**
     * Créer une réservation via REST
     */
    private Map<String, Object> makeReservation(Map<String, Object> payload) throws Exception {
        String hotelCode = str(payload.get("hotelCode"));
        String offerId = str(payload.get("offerId"));
        String agencyId = str(payload.get("agencyId"));
        String nom = str(payload.get("nom"));
        String prenom = str(payload.get("prenom"));
        String carte = str(payload.get("carte"));
        
        log.info("[AGENCY-REST] makeReservation hotelCode='{}' offerId='{}' agencyId='{}' nom='{}' prenom='{}' carte='{}'",
                hotelCode, offerId, agencyId, nom, prenom, maskCard(carte));
        
        if (hotelCode == null || hotelCode.trim().isEmpty()) {
            throw new IllegalArgumentException("hotelCode is required");
        }
        
        // Appel REST au serveur d'hôtel
        ReservationConfirmationDTO confirmation = hotelClient.makeReservation(
            hotelCode, offerId, agencyId, nom, prenom, carte);
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", confirmation.isSuccess());
        data.put("message", confirmation.getMessage());
        data.put("reference", confirmation.getId());
        
        return data;
    }
    
    // Méthodes utilitaires
    
    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
    
    private static int num(Object o, int defaultValue) {
        try {
            return o == null ? defaultValue : Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private static String maskCard(String c) {
        if (c == null) return null;
        String n = c.replaceAll("[^0-9]", "");
        if (n.length() < 4) return "****";
        return "**** **** **** " + n.substring(n.length() - 4);
    }
}

