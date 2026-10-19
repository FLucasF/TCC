package com.pousadamarealta.domain;

public class CnpjIdentification implements GuestIdentification {
    private final String cnpj;
    private final String companyName;

    public CnpjIdentification(String cnpj, String companyName) {
        this.cnpj = cnpj;
        this.companyName = companyName;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getCompanyName() {
        return companyName;
    }

    @Override
    public String formatForDocument() {
        return "CNPJ " + cnpj;
    }
}
