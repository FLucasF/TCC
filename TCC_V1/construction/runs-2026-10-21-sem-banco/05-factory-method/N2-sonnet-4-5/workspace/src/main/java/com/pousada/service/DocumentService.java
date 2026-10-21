package com.pousada.service;

import com.pousada.document.DocumentGeneratorFactory;
import com.pousada.domain.Reservation;

public class DocumentService {

    public String generateDocument(Reservation reservation) {
        var generator = DocumentGeneratorFactory.getGenerator(reservation.getIdentification());
        return generator.generate(reservation);
    }
}
