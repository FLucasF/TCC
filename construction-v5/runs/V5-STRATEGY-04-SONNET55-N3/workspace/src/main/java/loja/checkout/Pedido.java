package loja.checkout;

import java.util.List;

/** Dados recebidos do site, como texto/números crus. */
public record Pedido(
        List<ItemPedido> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record ItemPedido(String nome, java.math.BigDecimal precoUnitario, Integer quantidade,
                             java.math.BigDecimal pesoKg) {
    }
}
