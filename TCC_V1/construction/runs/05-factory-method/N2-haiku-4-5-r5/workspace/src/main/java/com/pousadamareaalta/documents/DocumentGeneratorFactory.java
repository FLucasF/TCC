package com.pousadamareaalta.documents;

import com.pousadamareaalta.models.Reservation;

public class DocumentGeneratorFactory {
    public static DocumentGenerator create(Reservation reservation) {
        if (reservation.guestCpf != null) {
            return new CpfDocument();
        } else if (reservation.companyCnpj != null) {
            return new CnpjDocument();
        } else if (reservation.guestPassport != null) {
            return new PassportDocument();
        }
        throw new IllegalArgumentException("Unknown reservation type");
    }
}
