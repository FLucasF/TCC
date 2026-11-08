package com.pousada.marealta.domain.reservation;

import com.pousada.marealta.domain.shared.DateRange;
import com.pousada.marealta.domain.shared.Guest;
import com.pousada.marealta.domain.shared.Money;
import com.pousada.marealta.domain.pricing.RatePlan;
import com.pousada.marealta.domain.room.Room;
import java.time.LocalDateTime;
import java.util.Objects;

public class Reservation {
    private final long id;
    private final Room room;
    private final Guest guest;
    private final DateRange dates;
    private final int guestCount;
    private final RatePlan ratePlan;
    private final Money subtotal;
    private final Money discount;
    private final Money total;
    private ReservationStatus status;
    private Money amountPaid;
    private Money refund;
    private final LocalDateTime createdAt;

    public Reservation(
            long id,
            Room room,
            Guest guest,
            DateRange dates,
            int guestCount,
            RatePlan ratePlan,
            Money subtotal,
            Money discount,
            Money total
    ) {
        Objects.requireNonNull(room, "Room cannot be null");
        Objects.requireNonNull(guest, "Guest cannot be null");
        Objects.requireNonNull(dates, "Date range cannot be null");
        Objects.requireNonNull(ratePlan, "Rate plan cannot be null");
        Objects.requireNonNull(subtotal, "Subtotal cannot be null");
        Objects.requireNonNull(discount, "Discount cannot be null");
        Objects.requireNonNull(total, "Total cannot be null");

        if (guestCount < 1) {
            throw new IllegalArgumentException("Guest count must be at least 1");
        }
        if (guestCount > room.capacity()) {
            throw new IllegalArgumentException("Guest count exceeds room capacity");
        }

        this.id = id;
        this.room = room;
        this.guest = guest;
        this.dates = dates;
        this.guestCount = guestCount;
        this.ratePlan = ratePlan;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = ReservationStatus.PENDING;
        this.amountPaid = Money.ofCents(0);
        this.refund = null;
        this.createdAt = LocalDateTime.now();
    }

    public long id() {
        return id;
    }

    public Room room() {
        return room;
    }

    public Guest guest() {
        return guest;
    }

    public DateRange dates() {
        return dates;
    }

    public int guestCount() {
        return guestCount;
    }

    public RatePlan ratePlan() {
        return ratePlan;
    }

    public Money subtotal() {
        return subtotal;
    }

    public Money discount() {
        return discount;
    }

    public Money total() {
        return total;
    }

    public ReservationStatus status() {
        return status;
    }

    public Money amountPaid() {
        return amountPaid;
    }

    public Money refund() {
        return refund;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public void confirm(Money amountPaid) {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm reservation with status " + status);
        }
        Objects.requireNonNull(amountPaid, "Amount paid cannot be null");
        if (amountPaid.doubleValue() <= 0) {
            throw new IllegalArgumentException("Amount paid must be positive");
        }

        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = amountPaid;
    }

    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot check in reservation with status " + status);
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Cannot check out reservation with status " + status);
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status == ReservationStatus.CHECKED_OUT || status == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel reservation with status " + status);
        }

        if (status == ReservationStatus.CONFIRMED) {
            // Calculate refund based on rate plan
            if (ratePlan == RatePlan.STANDARD) {
                this.refund = amountPaid;
            } else if (ratePlan == RatePlan.NON_REFUNDABLE) {
                this.refund = Money.ofCents(0);
            }
        } else if (status == ReservationStatus.PENDING) {
            // No payment was made
            this.refund = Money.ofCents(0);
        } else if (status == ReservationStatus.CHECKED_IN) {
            // Cannot cancel during or after check-in
            throw new IllegalStateException("Cannot cancel a checked-in reservation");
        }

        this.status = ReservationStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation)) return false;
        Reservation that = (Reservation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
