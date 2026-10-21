package com.pousada.document;

import com.pousada.domain.Reservation;

public interface DocumentRenderer {
    DocumentContent render(Reservation reservation);
}
