package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** As parcelas do resumo já calculadas, antes do ajuste da forma de pagamento. */
public record ValoresPedido(BigDecimal subtotalProdutos, BigDecimal descontoCupom, BigDecimal frete,
                            BigDecimal imposto) {

    /** Produtos − cupom + frete: a base das regras que não olham imposto. */
    public BigDecimal totalSemImposto() {
        return Dinheiro.centavos(subtotalProdutos.subtract(descontoCupom).add(frete));
    }

    /** Total do pedido. */
    public BigDecimal total() {
        return Dinheiro.centavos(totalSemImposto().add(imposto));
    }
}
