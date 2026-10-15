package br.com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

/** Dados da compra como enviados pelo site. */
public record PedidoCheckout(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public record Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
    }
}
