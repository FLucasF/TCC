package com.pousada.document;

import com.pousada.domain.Reservation;

public class DocumentGeneratorFactory {
    private static final CpfDocumentGenerator CPF_GENERATOR = new CpfDocumentGenerator();
    private static final CnpjDocumentGenerator CNPJ_GENERATOR = new CnpjDocumentGenerator();
    private static final PassportDocumentGenerator PASSPORT_GENERATOR = new PassportDocumentGenerator();

    public static DocumentGenerator create(Reservation reservation) {
        if (reservation.getGuestCpf() != null) {
            return CPF_GENERATOR;
        } else if (reservation.getCompanyCnpj() != null) {
            return CNPJ_GENERATOR;
        } else if (reservation.getGuestPassport() != null) {
            return PASSPORT_GENERATOR;
        }
        throw new IllegalArgumentException("Invalid reservation identification");
    }
}
