package com.loja.checkout.dominio;

import java.util.List;

/**
 * A compra como o site envia. Os códigos chegam como texto e são resolvidos pelos
 * catálogos, para que um valor inexistente vire o código de erro certo.
 */
public record PedidoInformado(
        List<ItemInformado> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
