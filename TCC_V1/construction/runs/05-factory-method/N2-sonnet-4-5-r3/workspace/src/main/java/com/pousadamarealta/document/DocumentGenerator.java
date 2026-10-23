package com.pousadamarealta.document;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

public interface DocumentGenerator {
    Document generate(Reservation reservation, Room room);
}
