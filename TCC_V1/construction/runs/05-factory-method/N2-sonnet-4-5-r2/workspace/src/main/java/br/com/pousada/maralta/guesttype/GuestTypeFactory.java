package br.com.pousada.maralta.guesttype;

import java.util.Map;

public final class GuestTypeFactory {

    public static GuestType createFromRequest(Map<String, Object> fields) {
        String cpf = (String) fields.get("guestCpf");
        String cnpj = (String) fields.get("companyCnpj");
        String companyName = (String) fields.get("companyName");
        String passport = (String) fields.get("guestPassport");
        String country = (String) fields.get("guestCountry");

        int providedFields = 0;
        if (cpf != null) providedFields++;
        if (cnpj != null && companyName != null) providedFields++;
        if (passport != null && country != null) providedFields++;

        if (providedFields != 1) {
            return null;
        }

        if (cpf != null) {
            return new IndividualGuest(cpf);
        }

        if (cnpj != null && companyName != null) {
            return new CorporateGuest(cnpj, companyName);
        }

        if (passport != null && country != null) {
            return new ForeignGuest(passport, country);
        }

        return null;
    }

    public static GuestType createFromDatabase(
        String cpf,
        String cnpj,
        String companyName,
        String passport,
        String country
    ) {
        if (cpf != null) {
            return new IndividualGuest(cpf);
        }

        if (cnpj != null) {
            return new CorporateGuest(cnpj, companyName);
        }

        if (passport != null) {
            return new ForeignGuest(passport, country);
        }

        throw new IllegalStateException("Invalid guest type data");
    }
}
