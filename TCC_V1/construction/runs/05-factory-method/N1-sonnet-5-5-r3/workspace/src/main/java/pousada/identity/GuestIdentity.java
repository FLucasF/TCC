package pousada.identity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Como o hóspede se identifica na reserva. Cada forma de identificação sabe quais campos traz
 * e como é o documento que ela recebe; nova forma = nova implementação + uma linha em {@link IdentityKind}.
 */
public interface GuestIdentity {

    IdentityKind kind();

    /** Os campos de identificação que esta forma preenche, pelo nome que aparecem na API. */
    Map<String, String> fields();

    DocumentLanguage language();

    /** Título do documento, sem o número. */
    String documentTitle();

    /** Linhas que dizem quem é o hóspede (e a empresa, se houver), logo após o título. */
    List<String> partyLines(String guestName);

    /** Linhas depois do total. */
    default List<String> footerLines(BigDecimal total) {
        return List.of();
    }
}
