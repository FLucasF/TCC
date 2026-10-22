package hotel.reservations;

import hotel.documents.DocumentLayout;

/** How a guest is identified; each way brings along the document the guest gets. */
public interface Identification {

    IdentificationFields fields();

    DocumentLayout documentLayout();
}
