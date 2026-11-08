package br.com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

public class Reservation {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private Long id;
    private String roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private RatePlan ratePlan;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(String roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = ZERO;
    }

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

    public int getNights() {
        return (int) checkIn.datesUntil(checkOut).count();
    }

    public BigDecimal calculateNightPrice(LocalDate night, BigDecimal baseRate) {
        BigDecimal price = baseRate;
        if (isHighSeason(night)) {
            price = price.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return price.setScale(2, RoundingMode.HALF_EVEN);
    }

    private boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }
        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }
        if (month == Month.JULY) {
            return true;
        }
        return false;
    }

    public BigDecimal calculateSubtotal(BigDecimal baseRate) {
        BigDecimal subtotal = ZERO;
        LocalDate night = checkIn;
        while (night.isBefore(checkOut)) {
            subtotal = subtotal.add(calculateNightPrice(night, baseRate));
            night = night.plusDays(1);
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        BigDecimal discount = subtotal.multiply(ratePlan.getDiscountRate());
        return discount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateTotal(BigDecimal baseRate) {
        BigDecimal subtotal = calculateSubtotal(baseRate);
        BigDecimal discount = calculateDiscount(subtotal);
        return subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
    }

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can check in");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in reservations can check out");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed reservations can be cancelled");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    public void setAmountPaid(BigDecimal amount) {
        this.amountPaid = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public void setRefund(BigDecimal refund) {
        this.refund = refund.setScale(2, RoundingMode.HALF_EVEN);
    }

    public BigDecimal calculateRefund() {
        if (ratePlan == RatePlan.STANDARD) {
            return amountPaid;
        }
        return ZERO;
    }

    public String formatCpf() {
        if (guestCpf.length() == 11) {
            return guestCpf.substring(0, 3) + "." +
                   guestCpf.substring(3, 6) + "." +
                   guestCpf.substring(6, 9) + "-" +
                   guestCpf.substring(9, 11);
        }
        return guestCpf;
    }

    public String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    public String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    public String generateReceipt(RoomType roomType, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(id).append("\n");
        receipt.append("Hóspede: ").append(guestName).append(" (CPF ").append(formatCpf()).append(")\n");
        receipt.append("Quarto: ").append(roomNumber).append(" (").append(roomType.name()).append(")\n");
        receipt.append("Entrada: ").append(formatDate(checkIn));
        receipt.append(" | Saída: ").append(formatDate(checkOut));
        receipt.append(" | ").append(getNights()).append(getNights() == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: ").append(formatMoney(subtotal)).append("\n");

        if (discount.compareTo(ZERO) > 0) {
            receipt.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }

        receipt.append("Total: ").append(formatMoney(total));

        return receipt.toString();
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
        return amountPaid;
    }

    public BigDecimal getRefund() {
        return refund;
    }

    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    public boolean wasConfirmed() {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT ||
               (status == ReservationStatus.CANCELLED && amountPaid.compareTo(ZERO) > 0);
    }
}
