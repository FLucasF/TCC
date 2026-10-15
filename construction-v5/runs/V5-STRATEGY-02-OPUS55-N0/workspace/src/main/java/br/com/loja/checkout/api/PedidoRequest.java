package br.com.loja.checkout.api;

import java.math.BigDecimal;
import java.util.List;

public record PedidoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        BigDecimal parcelas,
        String nivelClube,
        String regiao) {
}
