package com.pousadamaretalta;

/**
 * Como o hóspede se identifica. Cada tipo escolhe o documento que recebe.
 * Um novo tipo (ex.: agência de turismo) é um novo record aqui, um novo
 * {@link DocumentTemplate} e uma entrada em {@link IdentityReader#ALL}.
 */
public interface GuestIdentity {
    DocumentTemplate template();

    default String cpf() { return null; }
    default String cnpj() { return null; }
    default String companyName() { return null; }
    default String passport() { return null; }
    default String country() { return null; }

    record Person(String cpf) implements GuestIdentity {
        public DocumentTemplate template() { return DocumentTemplate.COMPROVANTE; }
    }

    record Company(String cnpj, String companyName) implements GuestIdentity {
        public DocumentTemplate template() { return DocumentTemplate.FATURA; }
    }

    record Foreigner(String passport, String country) implements GuestIdentity {
        public DocumentTemplate template() { return DocumentTemplate.BOOKING_RECEIPT; }
    }
}
