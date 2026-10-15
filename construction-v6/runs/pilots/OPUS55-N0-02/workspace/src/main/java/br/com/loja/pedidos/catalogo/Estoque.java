package br.com.loja.pedidos.catalogo;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baixa e devolução de estoque. Sempre roda dentro da transação de quem chama: as linhas dos
 * produtos ficam travadas até o fim dela, então dois pagamentos simultâneos da última peça
 * nunca conseguem baixar o mesmo estoque (e o banco ainda recusa estoque negativo).
 */
@Component
public class Estoque {

    private final JdbcClient jdbc;

    public Estoque(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Baixa as quantidades (sku → quantidade) se houver estoque para todas.
     *
     * @return {@code false}, sem baixar nada, se faltar estoque de algum produto
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean baixar(Map<String, Integer> quantidades) {
        Map<String, Integer> emOrdem = new TreeMap<>(quantidades);
        // Trava sempre na mesma ordem (por SKU) para evitar impasse entre pagamentos simultâneos.
        Map<String, Integer> disponivel = new TreeMap<>();
        for (String sku : emOrdem.keySet()) {
            jdbc.sql("SELECT estoque FROM produto WHERE sku = :sku FOR UPDATE")
                    .param("sku", sku)
                    .query(Integer.class)
                    .optional()
                    .ifPresent(estoque -> disponivel.put(sku, estoque));
        }
        boolean suficiente = emOrdem.entrySet().stream()
                .allMatch(item -> disponivel.getOrDefault(item.getKey(), 0) >= item.getValue());
        if (!suficiente) {
            return false;
        }
        emOrdem.forEach((sku, quantidade) -> {
            int alterados = jdbc.sql("UPDATE produto SET estoque = estoque - :quantidade WHERE sku = :sku AND estoque >= :quantidade")
                    .param("quantidade", quantidade)
                    .param("sku", sku)
                    .update();
            if (alterados != 1) {
                throw new IllegalStateException("Estoque mudou durante a baixa do produto " + sku);
            }
        });
        return true;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void devolver(Map<String, Integer> quantidades) {
        new TreeMap<>(quantidades).forEach((sku, quantidade) ->
                jdbc.sql("UPDATE produto SET estoque = estoque + :quantidade WHERE sku = :sku")
                        .param("quantidade", quantidade)
                        .param("sku", sku)
                        .update());
    }
}
