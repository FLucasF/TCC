package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal DESCONTO = Dinheiro.reais("50.00");
    private static final BigDecimal MINIMO_EM_PRODUTOS = Dinheiro.reais("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return pedido.subtotalProdutos().compareTo(MINIMO_EM_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return DESCONTO;
    }
}
