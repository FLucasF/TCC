package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Carrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Menos50Cupom implements Cupom {

    private static final BigDecimal VALOR_MINIMO_PRODUTOS = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return carrinho.subtotalProdutos().compareTo(VALOR_MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return DESCONTO;
    }
}
