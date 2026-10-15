package br.com.loja.checkout.dominio;

import java.util.List;

/** Os dados da compra, como o site os envia. */
public record Compra(List<Item> itens,
                     String modalidadeEntrega,
                     String cupom,
                     String formaPagamento,
                     Integer parcelas,
                     String nivelClube,
                     String regiao) {

    public int parcelasOuUma() {
        return parcelas == null ? 1 : parcelas;
    }
}
