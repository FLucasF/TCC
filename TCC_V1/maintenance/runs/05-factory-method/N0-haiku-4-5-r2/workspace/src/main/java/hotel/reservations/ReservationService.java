package hotel.reservations;

import hotel.documents.BookingReceipt;
import hotel.documents.Invoice;
import hotel.documents.ReservationReceipt;
import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.math.BigDecimal;

/** The life of a reservation: created, confirmed (paid), checked in, checked out, or cancelled. */
public final class ReservationService {

    static final String HOTEL_NAME = "Pousada Maré Alta";

    private final Rooms rooms;
    private final ReservationStore store;
    private final PriceCalculator priceCalculator;
    private final EmailSender emailSender;

    public ReservationService(
            Rooms rooms, ReservationStore store, PriceCalculator priceCalculator, EmailSender emailSender) {
        this.rooms = rooms;
        this.store = store;
        this.priceCalculator = priceCalculator;
        this.emailSender = emailSender;
    }

    public synchronized Reservation create(NewReservation request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "check-out must be after check-in");
        }
        if (request.guests() < 1) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "a reservation needs at least one guest");
        }
        Room room = rooms.find(request.roomNumber())
                .orElseThrow(() -> new HotelException(
                        ErrorCode.ROOM_NOT_FOUND, "room " + request.roomNumber() + " does not exist"));
        if (request.guests() > room.capacity()) {
            throw new HotelException(
                    ErrorCode.CAPACITY_EXCEEDED,
                    "room " + room.number() + " takes at most " + room.capacity() + " guests");
        }
        if (store.isRoomTaken(room.number(), request.checkIn(), request.checkOut())) {
            throw new HotelException(
                    ErrorCode.ROOM_UNAVAILABLE, "room " + room.number() + " is taken on some of those nights");
        }
        Price price = priceCalculator.calculate(room, request.checkIn(), request.checkOut(), request.ratePlan());
        return store.add(request, price);
    }

    public Reservation find(long reservationId) {
        return store.find(reservationId)
                .orElseThrow(() -> new HotelException(
                        ErrorCode.RESERVATION_NOT_FOUND, "reservation " + reservationId + " does not exist"));
    }

    public String getDocumentText(Reservation reservation) {
        return generateDocumentText(reservation, roomOf(reservation));
    }

    /** The guest paid: the reservation is confirmed and the guest gets the receipt by e-mail. */
    public synchronized Reservation confirm(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.PENDING) {
            throw notAllowed(reservation, "confirm");
        }
        reservation.confirmPayment();
        store.update(reservation);
        String documentText = generateDocumentText(reservation, roomOf(reservation));
        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - " + HOTEL_NAME,
                confirmationEmailBody(reservation, documentText));
        return reservation;
    }

    /**
     * Cancelling before the stay starts. What was paid comes back according to the rate plan (a pending
     * reservation was never paid, so nothing comes back).
     */
    public synchronized Reservation cancel(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.PENDING
                && reservation.status() != ReservationStatus.CONFIRMED) {
            throw notAllowed(reservation, "cancel");
        }
        BigDecimal refund = switch (reservation.ratePlan()) {
            case STANDARD -> reservation.amountPaid();
            case NON_REFUNDABLE -> PriceCalculator.cents(BigDecimal.ZERO);
        };
        reservation.cancel(refund);
        store.update(reservation);
        return reservation;
    }

    public synchronized Reservation checkIn(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.CONFIRMED) {
            throw notAllowed(reservation, "check in");
        }
        reservation.changeStatus(ReservationStatus.CHECKED_IN);
        store.update(reservation);
        return reservation;
    }

    /** The guest leaves: the stay is closed and the final receipt is handed over. */
    public synchronized Object checkOut(long reservationId) {
        Reservation reservation = find(reservationId);
        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            throw notAllowed(reservation, "check out");
        }
        reservation.changeStatus(ReservationStatus.CHECKED_OUT);
        store.update(reservation);
        return generateDocument(reservation, roomOf(reservation));
    }

    /** A copy of the receipt, for a guest who lost it. Only paid reservations have one. */
    public Object receiptCopy(long reservationId) {
        Reservation reservation = find(reservationId);
        if (!reservation.isPaid()) {
            throw notAllowed(reservation, "issue a receipt for");
        }
        return generateDocument(reservation, roomOf(reservation));
    }

    private Room roomOf(Reservation reservation) {
        return rooms.find(reservation.roomNumber()).orElseThrow();
    }

    private Object generateDocument(Reservation reservation, Room room) {
        if (reservation.guest().cpf() != null) {
            return new ReservationReceipt(reservation, room);
        } else if (reservation.guest().companyCnpj() != null) {
            BigDecimal issRetained = reservation.price().subtotal()
                    .multiply(PriceCalculator.cents(new BigDecimal("5")))
                    .divide(PriceCalculator.cents(new BigDecimal("100")));
            return new Invoice(reservation, room, issRetained);
        } else {
            return new BookingReceipt(reservation, room);
        }
    }

    private String generateDocumentText(Reservation reservation, Room room) {
        if (reservation.guest().cpf() != null) {
            return generateReservationReceiptText(reservation, room);
        } else if (reservation.guest().companyCnpj() != null) {
            return generateInvoiceText(reservation, room);
        } else {
            return generateBookingReceiptText(reservation, room);
        }
    }

    private static String generateReservationReceiptText(Reservation reservation, Room room) {
        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservation.id()).append('\n');
        text.append("Hóspede: ").append(reservation.guest().name()).append(" (CPF ").append(reservation.guest().cpf()).append(")\n");
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(reservation.price().nights()).append(reservation.price().nights() == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(reservation.price().subtotal())).append('\n');
        if (reservation.price().discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(reservation.price().discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(reservation.price().total())).append('\n');
        return text.toString();
    }

    private static String generateInvoiceText(Reservation reservation, Room room) {
        BigDecimal issRetained = reservation.price().subtotal()
                .multiply(PriceCalculator.cents(new BigDecimal("5")))
                .divide(PriceCalculator.cents(new BigDecimal("100")));

        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservation.id()).append('\n');
        text.append("Empresa: ").append(reservation.guest().companyName()).append(" (CNPJ ").append(reservation.guest().companyCnpj()).append(")\n");
        text.append("Hóspede: ").append(reservation.guest().name()).append('\n');
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(reservation.price().nights()).append(reservation.price().nights() == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(reservation.price().subtotal())).append('\n');
        text.append("Total: ").append(TextFormat.money(reservation.price().subtotal())).append('\n');
        text.append("ISS retido (5%): ").append(TextFormat.money(issRetained)).append('\n');
        return text.toString();
    }

    private static String generateBookingReceiptText(Reservation reservation, Room room) {
        java.time.format.DateTimeFormatter isoDate = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservation.id()).append('\n');
        text.append("Guest: ").append(reservation.guest().name()).append(" (passport ").append(reservation.guest().guestPassport())
                .append(", ").append(reservation.guest().guestCountry()).append(")\n");
        text.append("Room: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Check-in: ").append(isoDate.format(reservation.checkIn()))
                .append(" | Check-out: ").append(isoDate.format(reservation.checkOut()))
                .append(" | ").append(reservation.price().nights()).append(reservation.price().nights() == 1 ? " night" : " nights").append('\n');
        text.append("Nights: ").append(englishMoney(reservation.price().subtotal())).append('\n');
        if (reservation.price().discount().signum() > 0) {
            text.append("Discount: ").append(englishMoney(reservation.price().discount())).append('\n');
        }
        text.append("Total: ").append(englishMoney(reservation.price().total())).append('\n');
        return text.toString();
    }

    private static String englishMoney(BigDecimal amount) {
        return "BRL " + String.format(java.util.Locale.US, "%,.2f", amount);
    }

    private static String confirmationEmailBody(Reservation reservation, String documentText) {
        return "Olá, " + reservation.guest().name() + "!\n\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\n\n"
                + documentText + "\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!\n";
    }

    private static HotelException notAllowed(Reservation reservation, String operation) {
        return new HotelException(
                ErrorCode.OPERATION_NOT_ALLOWED,
                "cannot " + operation + " reservation " + reservation.id() + " while it is " + reservation.status());
    }
}
