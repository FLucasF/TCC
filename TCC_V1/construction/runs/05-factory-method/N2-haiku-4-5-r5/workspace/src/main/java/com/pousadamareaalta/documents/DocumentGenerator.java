package com.pousadamareaalta.documents;

import com.pousadamareaalta.models.Reservation;
import com.pousadamareaalta.models.Room;

public interface DocumentGenerator {
    String generate(Reservation reservation, Room room, long reservationId);
    String getTitle();
}
