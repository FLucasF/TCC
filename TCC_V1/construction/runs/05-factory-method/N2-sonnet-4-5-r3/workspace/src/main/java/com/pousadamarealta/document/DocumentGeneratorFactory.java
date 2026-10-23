package com.pousadamarealta.document;

import com.pousadamarealta.domain.Reservation;

public class DocumentGeneratorFactory {
    private final IndividualDocumentGenerator individualGenerator = new IndividualDocumentGenerator();
    private final CorporateDocumentGenerator corporateGenerator = new CorporateDocumentGenerator();
    private final InternationalDocumentGenerator internationalGenerator = new InternationalDocumentGenerator();

    public DocumentGenerator getGenerator(Reservation reservation) {
        if (reservation.hasIndividualGuest()) {
            return individualGenerator;
        }
        if (reservation.hasCorporateGuest()) {
            return corporateGenerator;
        }
        if (reservation.hasInternationalGuest()) {
            return internationalGenerator;
        }
        throw new IllegalStateException("Reservation must have guest identification");
    }
}
