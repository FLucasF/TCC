package br.com.marealta.identity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Um jeito de o hóspede se identificar. Cada jeito sabe quais campos da requisição exige e como é o
 * documento que ele recebe; o resto do sistema só conhece esta interface.
 */
public interface IdentityKind {
    /** Valor gravado no banco. */
    String code();

    /** Campos da requisição que identificam este jeito (todos obrigatórios, e nenhum outro de identificação). */
    List<String> fieldNames();

    String documentTitle();

    Language language();

    /** Linhas que identificam quem recebe o documento, logo depois do título. */
    List<String> identityLines(String guestName, Map<String, String> values);

    /** Linhas depois do total. */
    default List<String> closingLines(BigDecimal total) {
        return List.of();
    }
}
