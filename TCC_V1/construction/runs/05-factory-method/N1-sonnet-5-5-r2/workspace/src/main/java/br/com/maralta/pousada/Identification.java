package br.com.maralta.pousada;

import java.util.List;
import java.util.Map;

/**
 * Como o hóspede se identifica. Cada forma de identificação conhece os seus campos e o documento que
 * a reserva recebe (confirmação, check-out e segunda via). Para acrescentar uma nova, basta criar a classe
 * e listá-la em {@link Identifications#KINDS}.
 */
public interface Identification {
    /** Campos desta identificação, pelo nome que têm na API. */
    Map<String, String> fields();

    /** Idioma e formato do documento. */
    Language language();

    /** Título do documento, sem o número. */
    String title();

    /** Linhas entre o título e o quarto, que identificam quem é o hóspede (e a empresa, se houver). */
    List<String> partyLines(String guestName);

    /** Linhas depois do total. */
    default List<String> footerLines(Document document) {
        return List.of();
    }
}
