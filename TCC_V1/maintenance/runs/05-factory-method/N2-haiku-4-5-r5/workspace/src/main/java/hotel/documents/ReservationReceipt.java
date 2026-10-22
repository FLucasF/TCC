package hotel.documents;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    defaultImpl = CpfReceipt.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CpfReceipt.class, name = "CPF"),
    @JsonSubTypes.Type(value = CnpjInvoice.class, name = "CNPJ"),
    @JsonSubTypes.Type(value = PassportReceipt.class, name = "PASSPORT")
})

/** Base class for reservation documents. Different identification types generate different documents. */
public sealed abstract class ReservationReceipt permits CpfReceipt, CnpjInvoice, PassportReceipt {

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

    protected ReservationReceipt(
            long reservationId,
            String guestName,
            int roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total) {
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
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

    @JsonProperty("text")
    public abstract String text();

    @JsonProperty("title")
    public abstract String title();
}
