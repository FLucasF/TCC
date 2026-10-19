package com.pousadamareaalta.document;

import com.pousadamareaalta.model.Document;
import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.model.Room;

public interface DocumentGenerator {
    Document generate(Reservation reservation, Room room);
}
