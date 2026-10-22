package br.com.marealta;

import java.util.List;

public class CnpjIdentity implements IdentityType {
    public static final String CNPJ = "companyCnpj";
    public static final String NAME = "companyName";

    public String kind() { return "CNPJ"; }

    public List<String> fields() { return List.of(CNPJ, NAME); }

    public DocumentFormat format() { return new InvoiceFormat(); }
}
