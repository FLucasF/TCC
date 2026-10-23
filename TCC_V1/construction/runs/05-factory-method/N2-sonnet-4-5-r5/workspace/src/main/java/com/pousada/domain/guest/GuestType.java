package com.pousada.domain.guest;

import com.pousada.domain.model.Document;
import com.pousada.domain.model.Reservation;

public interface GuestType {
    Document generateDocument(Reservation reservation);

    String guestCpf();
    String companyCnpj();
    String companyName();
    String guestPassport();
    String guestCountry();
}
