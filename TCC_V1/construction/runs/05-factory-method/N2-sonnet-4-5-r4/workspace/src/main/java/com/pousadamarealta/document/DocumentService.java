package com.pousadamarealta.document;

import com.pousadamarealta.domain.GuestIdentification;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;

public final class DocumentService {
    private final IndividualGuestDocumentGenerator individualGenerator = new IndividualGuestDocumentGenerator();
    private final CorporateGuestDocumentGenerator corporateGenerator = new CorporateGuestDocumentGenerator();
    private final ForeignGuestDocumentGenerator foreignGenerator = new ForeignGuestDocumentGenerator();

    public Document generateDocument(Reservation reservation, RoomType roomType) {
        DocumentGenerator generator = selectGenerator(reservation.guestIdentification());
        return generator.generate(reservation, roomType);
    }

    private DocumentGenerator selectGenerator(GuestIdentification guestId) {
        if (guestId instanceof GuestIdentification.IndividualGuest) {
            return individualGenerator;
        } else if (guestId instanceof GuestIdentification.CorporateGuest) {
            return corporateGenerator;
        } else if (guestId instanceof GuestIdentification.ForeignGuest) {
            return foreignGenerator;
        }
        throw new IllegalArgumentException("Unknown guest identification type");
    }
}
