package br.com.loja.checkout.api;

import java.util.List;

/** Dados da compra enviados pelo site. Os códigos chegam como texto e são validados no cálculo. */
public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
