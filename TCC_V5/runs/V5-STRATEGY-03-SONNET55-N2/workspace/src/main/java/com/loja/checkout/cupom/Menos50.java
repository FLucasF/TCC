package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return pedido.subtotalProdutos().compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Dinheiro.arredondar(new BigDecimal("50.00"));
    }
}
