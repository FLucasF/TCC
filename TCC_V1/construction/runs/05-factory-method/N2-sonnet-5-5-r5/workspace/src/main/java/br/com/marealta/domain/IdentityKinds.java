package br.com.marealta.domain;

import java.util.List;

/** Registro dos jeitos de identificação aceitos; um jeito novo entra aqui. */
public final class IdentityKinds {
    private static final List<IdentityKind> KINDS = List.of(
            CpfIdentity.KIND,
            CnpjIdentity.KIND,
            PassportIdentity.KIND);

    private IdentityKinds() {
    }

    public static GuestIdentity from(IdentityFields fields) {
        List<IdentityKind> present = KINDS.stream().filter(k -> k.isPresent(fields)).toList();
        if (present.size() != 1) {
            throw DomainException.invalid(
                    "Informe exatamente uma identificação: guestCpf, companyCnpj com companyName, "
                            + "ou guestPassport com guestCountry");
        }
        return present.get(0).create(fields);
    }
}
