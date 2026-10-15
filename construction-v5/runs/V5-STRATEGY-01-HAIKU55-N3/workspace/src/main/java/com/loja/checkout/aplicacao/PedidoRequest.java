package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.Item;
import java.util.List;

public record PedidoRequest(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
