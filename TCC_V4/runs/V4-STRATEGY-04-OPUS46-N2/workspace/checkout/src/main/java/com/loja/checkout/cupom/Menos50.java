package com.loja.checkout.cupom;

import com.loja.checkout.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete) {
        return DESCONTO;
    }
}
