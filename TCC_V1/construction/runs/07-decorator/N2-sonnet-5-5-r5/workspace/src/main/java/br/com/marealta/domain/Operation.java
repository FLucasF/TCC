package br.com.marealta.domain;

import static br.com.marealta.domain.ReservationStatus.*;

import java.util.Set;

/** The status transitions allowed in the life of a reservation: one row per operation. */
public enum Operation {
    CONFIRM(Set.of(PENDING), CONFIRMED),
    CHECK_IN(Set.of(CONFIRMED), CHECKED_IN),
    CHECK_OUT(Set.of(CHECKED_IN), CHECKED_OUT),
    CANCEL(Set.of(PENDING, CONFIRMED), CANCELLED);

    private final Set<ReservationStatus> allowedFrom;
    private final ReservationStatus target;

    Operation(Set<ReservationStatus> allowedFrom, ReservationStatus target) {
        this.allowedFrom = allowedFrom;
        this.target = target;
    }

    public ReservationStatus apply(ReservationStatus current) {
        if (!allowedFrom.contains(current)) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Operation " + name() + " is not allowed for a reservation in status " + current);
        }
        return target;
    }
}
