package com.pousada.document;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;

public interface DocumentGenerator {
    Document generate(Reservation reservation, Room room);
}
