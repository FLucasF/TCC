package com.loja.checkout.web;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados da compra enviados pelo site para {@code /checkout/resumo}.
 *
 * <p>Os campos que representam opções (entrega, pagamento, nível, região, cupom) chegam
 * como texto cru e são validados no serviço, para que um valor desconhecido devolva o
 * código de erro certo em vez de uma falha genérica de leitura do JSON.
 */
public record CheckoutRequest(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    /**
     * Um produto do carrinho.
     *
     * @param nome          nome do produto
     * @param precoUnitario preço de uma unidade (número com ponto, ex.: 79.90)
     * @param quantidade    quantas unidades (inteiro)
     * @param pesoKg        peso de uma unidade em kg (número com ponto, ex.: 0.30)
     */
    public record Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
    }
}
