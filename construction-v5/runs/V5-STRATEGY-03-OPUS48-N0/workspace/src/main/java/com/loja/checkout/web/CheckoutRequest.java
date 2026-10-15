package com.loja.checkout.web;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados que o site envia para {@code /checkout/resumo}.
 *
 * <p>Os campos que identificam opcoes ({@code modalidadeEntrega}, {@code formaPagamento},
 * {@code nivelClube}, {@code regiao} e {@code cupom}) sao recebidos como texto, e nao como
 * enum, de proposito: assim conseguimos validar cada um na ordem certa e devolver o codigo
 * de erro exato quando vier um valor desconhecido ou ausente.</p>
 */
public record CheckoutRequest(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao
) {

    /** Um produto do carrinho. */
    public record Item(
            String nome,
            BigDecimal precoUnitario,
            Integer quantidade,
            BigDecimal pesoKg
    ) {
    }
}
