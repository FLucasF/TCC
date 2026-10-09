package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 50,00 de desconto nos produtos, de R$ 300,00 em produtos para cima. */
@Component
class Menos50 implements Cupom {

    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");
    private static final BigDecimal VALOR = new BigDecimal("50.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(BaseCupom base) {
        return base.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal desconto(BaseCupom base) {
        return Dinheiro.centavos(VALOR);
    }
}
