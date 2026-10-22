package br.com.mare.pousada;

import java.util.Map;

/** Identificação do hóspede; cada tipo sabe qual documento a reserva dele recebe. */
public interface GuestIdentity {
    IdentityKind kind();

    /** Campo do corpo da requisição -> valor. */
    Map<String, String> values();

    Document document(Reservation reservation);
}
