package pousada.domain.identification;

import pousada.domain.document.Document;
import pousada.domain.document.Voucher;

import java.util.Map;

/**
 * Como o hóspede se identifica na reserva. Cada forma sabe quais campos traz e qual
 * documento emite; uma forma nova entra criando a classe e registrando-a em
 * {@link IdentificationKinds}.
 */
public interface Identification {

    /** Somente os campos que se aplicam, pelo nome que o site usa. */
    Map<String, String> fields();

    Document issue(Voucher voucher);
}
