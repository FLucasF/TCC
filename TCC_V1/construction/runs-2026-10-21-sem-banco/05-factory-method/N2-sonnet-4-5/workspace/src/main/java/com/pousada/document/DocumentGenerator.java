package com.pousada.document;

import com.pousada.domain.Reservation;

public interface DocumentGenerator {
    String generate(Reservation reservation);
}
