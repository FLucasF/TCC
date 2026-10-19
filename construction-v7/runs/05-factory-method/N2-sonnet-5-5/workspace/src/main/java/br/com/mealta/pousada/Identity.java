package br.com.mealta.pousada;

import java.util.Map;

/**
 * Como o hóspede se identifica e qual documento ele recebe. Um novo jeito de reservar
 * (ex.: agência de turismo) é uma nova implementação, registrada em {@link IdentityTypes}.
 */
public interface Identity {
    /** Campos de identificação desta reserva (só os que se aplicam). */
    Map<String, String> fields();

    /** Título do documento no JSON. */
    String title();

    /** Texto do documento, igual ao que vai no e-mail. */
    String render(DocumentData data);
}
