package pousada.domain;

import pousada.domain.rateplan.PricingResult;
import pousada.domain.rateplan.RatePlan;
import pousada.domain.rateplan.RatePlanFactory;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private Long id;
    private final String roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final String ratePlanName;
    private final String companyCode;
    private final long nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, String roomNumber, String guestName, String guestEmail, String guestCpf,
                      LocalDate checkIn, LocalDate checkOut, int guests, String ratePlanName, String companyCode,
                      long nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                      ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlanName = ratePlanName;
        this.companyCode = companyCode;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation create(String roomNumber, RoomType roomType, String guestName, String guestEmail,
                                    String guestCpf, LocalDate checkIn, LocalDate checkOut, int guests,
                                    String ratePlanName, String companyCode) {
        RatePlan ratePlan = RatePlanFactory.get(ratePlanName);
        ratePlan.validate(checkIn, checkOut, roomType, companyCode);

        PricingResult pricing = ratePlan.calculatePricing(checkIn, checkOut, roomType, companyCode);
        long nights = checkOut.toEpochDay() - checkIn.toEpochDay();

        return new Reservation(
            null,
            roomNumber,
            guestName,
            guestEmail,
            guestCpf,
            checkIn,
            checkOut,
            guests,
            ratePlanName,
            companyCode,
            nights,
            pricing.subtotal(),
            pricing.discount(),
            pricing.total(),
            ReservationStatus.PENDING,
            BigDecimal.ZERO,
            null
        );
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Can only confirm a pending reservation");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = total;
    }

    public void doCheckIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Can only check-in a confirmed reservation");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void doCheckOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Can only check-out a checked-in reservation");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Can only cancel a pending or confirmed reservation");
        }
        this.status = ReservationStatus.CANCELLED;

        RatePlan ratePlan = RatePlanFactory.get(ratePlanName);
        this.refund = ratePlan.calculateRefund(amountPaid);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }

    public String roomNumber() {
        return roomNumber;
    }

    public String guestName() {
        return guestName;
    }

    public String guestEmail() {
        return guestEmail;
    }

    public String guestCpf() {
        return guestCpf;
    }

    public LocalDate checkIn() {
        return checkIn;
    }

    public LocalDate checkOut() {
        return checkOut;
    }

    public int guests() {
        return guests;
    }

    public String ratePlanName() {
        return ratePlanName;
    }

    public String companyCode() {
        return companyCode;
    }

    public long nights() {
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

    public ReservationStatus status() {
        return status;
    }

    public BigDecimal amountPaid() {
        return amountPaid;
    }

    public BigDecimal refund() {
        return refund;
    }

    public RoomType roomType() {
        return Room.typeFromNumber(roomNumber);
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    public boolean occupiesRoom(LocalDate night) {
        if (status == ReservationStatus.CANCELLED) {
            return false;
        }
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }
}
