package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.stream.Stream;

public class Reservation {
    private Long id;
    private final String roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, String roomNumber, String guestName, String guestEmail, String guestCpf,
                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                      ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    public static Reservation create(String roomNumber, String guestName, String guestEmail, String guestCpf,
                                    LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        return new Reservation(null, roomNumber, guestName, guestEmail, guestCpf,
                              checkIn, checkOut, guests, ratePlan,
                              ReservationStatus.PENDING, BigDecimal.ZERO, null);
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Só é possível confirmar uma reserva pendente");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = calculateTotal();
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Só é possível fazer check-in de uma reserva confirmada");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Só é possível fazer check-out de uma reserva com check-in");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Só é possível cancelar antes do check-in");
        }
        this.status = ReservationStatus.CANCELLED;
        this.refund = calculateRefund();
    }

    private BigDecimal calculateRefund() {
        if (ratePlan == RatePlan.NON_REFUNDABLE) {
            return BigDecimal.ZERO;
        }
        return amountPaid;
    }

    public int getNights() {
        return (int) checkIn.datesUntil(checkOut).count();
    }

    public BigDecimal calculateSubtotal() {
        return checkIn.datesUntil(checkOut)
                .map(this::calculateNightRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateNightRate(LocalDate night) {
        BigDecimal baseRate = getRoomNightlyRate();
        if (isHighSeason(night)) {
            baseRate = baseRate.multiply(new BigDecimal("1.20"));
        }
        return baseRate.setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }
        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }
        return false;
    }

    public BigDecimal calculateDiscount() {
        if (ratePlan != RatePlan.NON_REFUNDABLE) {
            return BigDecimal.ZERO;
        }
        BigDecimal subtotal = calculateSubtotal();
        return subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateTotal() {
        BigDecimal subtotal = calculateSubtotal();
        BigDecimal discount = calculateDiscount();
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateTourismTax() {
        BigDecimal taxPerGuestPerNight = new BigDecimal("4.50");
        return taxPerGuestPerNight
                .multiply(BigDecimal.valueOf(guests))
                .multiply(BigDecimal.valueOf(getNights()))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    public boolean occupiesNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public Stream<LocalDate> getNightStream() {
        return checkIn.datesUntil(checkOut);
    }

    public BigDecimal getNetRevenue() {
        BigDecimal paid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        BigDecimal returned = refund != null ? refund : BigDecimal.ZERO;
        return paid.subtract(returned);
    }

    private BigDecimal getRoomNightlyRate() {
        return switch (roomNumber) {
            case "101", "102" -> RoomType.STANDARD.getNightlyRate();
            case "201", "202" -> RoomType.DELUXE.getNightlyRate();
            case "301" -> RoomType.SUITE.getNightlyRate();
            default -> throw new IllegalStateException("Quarto desconhecido: " + roomNumber);
        };
    }

    public RoomType getRoomType() {
        return switch (roomNumber) {
            case "101", "102" -> RoomType.STANDARD;
            case "201", "202" -> RoomType.DELUXE;
            case "301" -> RoomType.SUITE;
            default -> throw new IllegalStateException("Quarto desconhecido: " + roomNumber);
        };
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public RatePlan getRatePlan() {
        return ratePlan;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid != null ? amountPaid : BigDecimal.ZERO;
    }

    public BigDecimal getRefund() {
        return refund;
    }
}
