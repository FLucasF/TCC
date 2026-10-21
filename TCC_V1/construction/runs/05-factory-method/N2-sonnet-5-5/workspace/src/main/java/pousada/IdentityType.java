package pousada;

import java.util.List;

/** Um jeito de se identificar na reserva: quais campos usa e como lê a identidade deles. */
public interface IdentityType {
    List<String> keys();

    GuestIdentity parse(RequestBody body);
}
