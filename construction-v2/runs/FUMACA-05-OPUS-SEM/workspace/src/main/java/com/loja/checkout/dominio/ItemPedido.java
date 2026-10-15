package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Item ja validado do carrinho. */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Preco x quantidade, sem arredondar (o arredondamento acontece no subtotal). */
    public BigDecimal valorBruto() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso x quantidade, sem arredondar. */
    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
