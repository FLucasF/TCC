package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.BaseCalculo;
import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 50,00 de desconto nos produtos, a partir de R$ 300,00 em produtos. */
@Component
public class CupomMenos50 implements PoliticaCupom {

    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(BaseCalculo base, BigDecimal frete) {
        return base.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal desconto(BaseCalculo base, BigDecimal frete) {
        return Dinheiro.arredondar(DESCONTO);
    }
}
