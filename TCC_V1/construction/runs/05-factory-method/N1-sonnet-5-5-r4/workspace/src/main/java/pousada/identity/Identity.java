package pousada.identity;

import pousada.document.IssuedDocument;
import pousada.document.Stay;

import java.util.Map;

/** Como o hóspede se identifica na reserva, e o documento que isso lhe dá direito a receber. */
public interface Identity {
    IdentityKind kind();

    /** Campos da identificação, pelo nome que o site usa. */
    Map<String, String> fields();

    IssuedDocument issue(Stay stay);
}
