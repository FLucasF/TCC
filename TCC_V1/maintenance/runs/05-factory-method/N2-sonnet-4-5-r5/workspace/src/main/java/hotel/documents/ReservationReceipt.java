package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The document the guest gets when the reservation is confirmed, at check-out, and on request. */
public final class ReservationReceipt {

    private final ReservationDocument document;

    public ReservationReceipt(Reservation reservation, Room room) {
        this.document = DocumentFactory.create(reservation, room);
    }

    public long reservationId() {
        return document.reservationId();
    }

    public String guestName() {
        return document.guestName();
    }

    public String guestCpf() {
        if (document instanceof IndividualReceipt individual) {
            return individual.guestCpf();
        }
        return null;
    }

    public String companyCnpj() {
        if (document instanceof CompanyInvoice company) {
            return company.companyCnpj();
        }
        return null;
    }

    public String companyName() {
        if (document instanceof CompanyInvoice company) {
            return company.companyName();
        }
        return null;
    }

    public String guestPassport() {
        if (document instanceof BookingReceipt booking) {
            return booking.guestPassport();
        }
        return null;
    }

    public String guestCountry() {
        if (document instanceof BookingReceipt booking) {
            return booking.guestCountry();
        }
        return null;
    }

    public BigDecimal iss() {
        if (document instanceof CompanyInvoice company) {
            return company.iss();
        }
        return null;
    }

    public int roomNumber() {
        return document.roomNumber();
    }

    public RoomType roomType() {
        return document.roomType();
    }

    public LocalDate checkIn() {
        return document.checkIn();
    }

    public LocalDate checkOut() {
        return document.checkOut();
    }

    public int nights() {
        return document.nights();
    }

    public BigDecimal subtotal() {
        return document.subtotal();
    }

    public BigDecimal discount() {
        return document.discount();
    }

    public BigDecimal total() {
        return document.total();
    }

    public String text() {
        return document.text();
    }
}
