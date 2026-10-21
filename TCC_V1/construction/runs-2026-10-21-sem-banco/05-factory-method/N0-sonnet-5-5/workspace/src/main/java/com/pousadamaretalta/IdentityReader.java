package com.pousadamaretalta;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Lê do corpo da requisição os campos de identificação; exige exatamente um conjunto. */
public final class IdentityReader {
    private record Kind(List<String> fields, Function<List<String>, GuestIdentity> build) {}

    private static final List<Kind> ALL = List.of(
            new Kind(List.of("guestCpf"), v -> new GuestIdentity.Person(v.get(0))),
            new Kind(List.of("companyCnpj", "companyName"), v -> new GuestIdentity.Company(v.get(0), v.get(1))),
            new Kind(List.of("guestPassport", "guestCountry"), v -> new GuestIdentity.Foreigner(v.get(0), v.get(1))));

    private IdentityReader() {}

    public static GuestIdentity read(JsonNode body) {
        GuestIdentity found = null;
        for (Kind kind : ALL) {
            List<String> values = new ArrayList<>();
            int present = 0;
            for (String f : kind.fields()) {
                String v = Json.optionalText(body, f);
                if (v != null) present++;
                values.add(v);
            }
            if (present == 0) continue;
            if (present != kind.fields().size() || found != null) {
                throw ApiException.invalid("Informe apenas um modo de identificação completo: guestCpf, "
                        + "companyCnpj + companyName, ou guestPassport + guestCountry");
            }
            found = kind.build().apply(values);
        }
        if (found == null) {
            throw ApiException.invalid("Identificação ausente: informe guestCpf, companyCnpj + companyName, "
                    + "ou guestPassport + guestCountry");
        }
        return found;
    }
}
