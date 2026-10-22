package pousada.identity;

import java.util.List;
import java.util.Map;

public interface IdentityKind {
    String name();

    /** Nomes dos campos desta identificação, como chegam do site. */
    List<String> fieldNames();

    /** Monta a identificação a partir dos campos informados; INVALID_REQUEST se faltar algum. */
    Identity create(Map<String, String> fields);
}
