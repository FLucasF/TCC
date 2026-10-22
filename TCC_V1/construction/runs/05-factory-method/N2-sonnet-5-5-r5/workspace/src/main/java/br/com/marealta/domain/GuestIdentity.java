package br.com.marealta.domain;

public interface GuestIdentity {
    IdentityFields fields();

    /** O documento que este tipo de hóspede recebe. */
    ReservationDocument documentFor(Reservation reservation);
}
