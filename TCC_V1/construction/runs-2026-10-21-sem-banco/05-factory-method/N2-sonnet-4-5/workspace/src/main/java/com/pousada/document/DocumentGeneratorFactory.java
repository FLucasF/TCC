package com.pousada.document;

import com.pousada.domain.CnpjIdentification;
import com.pousada.domain.CpfIdentification;
import com.pousada.domain.GuestIdentification;
import com.pousada.domain.PassportIdentification;

public class DocumentGeneratorFactory {

    public static DocumentGenerator getGenerator(GuestIdentification identification) {
        return switch (identification) {
            case CpfIdentification cpf -> new ComprovanteGenerator();
            case CnpjIdentification cnpj -> new FaturaGenerator();
            case PassportIdentification passport -> new ReceiptGenerator();
        };
    }
}
