package pousada;

import java.util.Map;

/** Como o hóspede se identificou, e o documento que essa identificação dá direito a receber. */
public interface GuestIdentity {
    /** Os campos de identificação que se aplicam, pelo nome que vai no JSON. */
    Map<String, String> fields();

    ReservationDocument issue(DocumentData data);
}
