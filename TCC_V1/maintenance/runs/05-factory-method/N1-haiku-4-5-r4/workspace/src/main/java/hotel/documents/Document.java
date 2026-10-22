package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public abstract sealed class Document permits ReservationReceipt, InvoiceDocument, BookingReceipt {

    protected final long reservationId;
    protected final String guestName;
    protected final String guestCpf;
    protected final String companyCnpj;
    protected final String companyName;
    protected final String guestPassport;
    protected final String guestCountry;
    protected final int roomNumber;
    protected final String roomType;
    protected final LocalDate checkIn;
    protected final LocalDate checkOut;
    protected final int nights;
    protected final BigDecimal subtotal;
    protected final BigDecimal discount;
    protected final BigDecimal total;

    protected Document(
            long reservationId,
            String guestName,
            String guestCpf,
            String companyCnpj,
            String companyName,
            String guestPassport,
            String guestCountry,
            int roomNumber,
            String roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total) {
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.guestCpf = guestCpf;
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
        this.guestPassport = guestPassport;
        this.guestCountry = guestCountry;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
    }

    public static Document of(Reservation reservation, Room room) {
        String cpf = reservation.guest().cpf();
        String cnpj = reservation.guest().companyCnpj();
        String company = reservation.guest().companyName();
        String passport = reservation.guest().guestPassport();
        String country = reservation.guest().guestCountry();

        if (cpf != null) {
            return new ReservationReceipt(reservation, room);
        } else if (cnpj != null) {
            return new InvoiceDocument(reservation, room);
        } else if (passport != null) {
            return new BookingReceipt(reservation, room);
        } else {
            throw new IllegalArgumentException("No valid identification provided");
        }
    }

    public long reservationId() {
        return reservationId;
    }

    public String guestName() {
        return guestName;
    }

    public String guestCpf() {
        return guestCpf;
    }

    public String companyCnpj() {
        return companyCnpj;
    }

    public String companyName() {
        return companyName;
    }

    public String guestPassport() {
        return guestPassport;
    }

    public String guestCountry() {
        return guestCountry;
    }

    public int roomNumber() {
        return roomNumber;
    }

    public String roomType() {
        return roomType;
    }

    public LocalDate checkIn() {
        return checkIn;
    }

    public LocalDate checkOut() {
        return checkOut;
    }

    public int nights() {
        return nights;
    }

    public BigDecimal subtotal() {
        return subtotal;
    }

    public BigDecimal discount() {
        return discount;
    }

    public BigDecimal total() {
        return total;
    }

    public abstract String text();

    public String title() {
        if (guestCpf != null) {
            return "Comprovante de reserva";
        } else if (companyCnpj != null) {
            return "Fatura de hospedagem";
        } else if (guestPassport != null) {
            return "Booking receipt";
        }
        return "";
    }
}
