package pousada.domain;

import java.util.Map;

/** Como o hóspede se identifica: o tipo (código do tipo de documento) e os campos informados. */
public record Identity(String type, Map<String, String> values) {
}
