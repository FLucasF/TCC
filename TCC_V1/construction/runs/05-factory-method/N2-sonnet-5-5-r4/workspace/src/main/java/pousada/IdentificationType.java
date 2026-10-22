package pousada;

import java.util.List;

/** Um jeito de o hóspede se identificar: os campos que exige e o documento que recebe. */
record IdentificationType(String code, List<String> fields, Document document) {
}
