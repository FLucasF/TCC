package com.pousada.domain;

public final class CnpjIdentification implements GuestIdentification {
    public String companyCnpj;
    public String companyName;

    public CnpjIdentification() {}
    public CnpjIdentification(String companyCnpj, String companyName) {
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
    }
}
