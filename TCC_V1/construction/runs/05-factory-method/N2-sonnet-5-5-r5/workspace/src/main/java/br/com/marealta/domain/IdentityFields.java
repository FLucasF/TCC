package br.com.marealta.domain;

/** Os cinco campos de identificação do hóspede; os que não se aplicam ficam null. */
public record IdentityFields(String guestCpf, String companyCnpj, String companyName,
                             String guestPassport, String guestCountry) {
}
