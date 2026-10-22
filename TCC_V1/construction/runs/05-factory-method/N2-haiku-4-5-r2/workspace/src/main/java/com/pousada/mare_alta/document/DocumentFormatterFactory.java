package com.pousada.mare_alta.document;

public class DocumentFormatterFactory {
    public static DocumentFormatter getFormatter(
            String guestCpf, String companyCnpj, String guestPassport) {

        if (guestCpf != null) {
            return new CpfDocumentFormatter();
        } else if (companyCnpj != null) {
            return new CnpjDocumentFormatter();
        } else if (guestPassport != null) {
            return new PassportDocumentFormatter();
        }

        throw new IllegalArgumentException("Invalid identification data");
    }
}
