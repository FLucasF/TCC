package pousada.identity;

import java.util.Map;

/** Como o hóspede se identifica; define também o documento que ele recebe. */
public interface GuestIdentity {
    /** Apenas os campos que se aplicam a esta identidade, pelo nome usado na API. */
    Map<String, String> fields();

    String documentTitle();

    String renderDocument(DocumentData data);
}
