package br.com.marealta;

import java.util.Map;

/** Como o hóspede se identifica: o tipo e os valores dos campos desse tipo. */
public record GuestIdentity(IdentityType type, Map<String, String> data) {
    public String get(String field) { return data.get(field); }

    public DocumentFormat format() { return type.format(); }
}
