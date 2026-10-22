package hotel.documents;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public sealed interface Document permits ReservationReceipt, InvoiceDocument, BookingReceipt {
    String text();

    long reservationId();
}
