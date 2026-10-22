package br.com.marealta;

import java.util.List;

public class CpfIdentity implements IdentityType {
    public static final String CPF = "guestCpf";

    public String kind() { return "CPF"; }

    public List<String> fields() { return List.of(CPF); }

    public DocumentFormat format() { return new ReceiptFormat(); }
}
