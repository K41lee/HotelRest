package org.examples.server.rest;

import dto.*;

import java.util.List;

/**
 * Interface du service REST pour les hôtels
 */
public interface HotelRestService {
    
    /**
     * Obtenir le catalogue de l'hôtel (nom, villes, agences)
     */
    CatalogDTO getCatalog();
    
    /**
     * Rechercher des offres selon des critères
     */
    List<OfferDTO> searchOffers(SearchRequestDTO criteria);
    
    /**
     * Créer une réservation
     */
    ReservationConfirmationDTO makeReservation(ReservationRequestDTO request);
}

