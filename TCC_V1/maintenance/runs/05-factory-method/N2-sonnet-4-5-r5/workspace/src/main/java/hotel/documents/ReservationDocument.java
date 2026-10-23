package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A document about a reservation, sent to the guest on confirmation, check-out, or on request. */
public abstract class ReservationDocument {

    protected final long reservationId;
    protected final String guestName;
    protected final int roomNumber;
    protected final RoomType roomType;
    protected final LocalDate checkIn;
    protected final LocalDate checkOut;
    protected final int nights;
    protected final BigDecimal subtotal;
    protected final BigDecimal discount;
    protected final BigDecimal total;

    protected ReservationDocument(Reservation reservation, Room room) {
        this.reservationId = reservation.id();
        this.guestName = reservation.guest().name();
        this.roomNumber = room.number();
        this.roomType = room.type();
        this.checkIn = reservation.checkIn();
        this.checkOut = reservation.checkOut();
        this.nights = reservation.price().nights();
        this.subtotal = reservation.price().subtotal();
        this.discount = reservation.price().discount();
        this.total = reservation.price().total();
    }

    public long reservationId() {
        return reservationId;
    }

    public String guestName() {
        return guestName;
    }

    public int roomNumber() {
        return roomNumber;
    }

    public RoomType roomType() {
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

    /** The full text of the document. */
    public abstract String text();
}
