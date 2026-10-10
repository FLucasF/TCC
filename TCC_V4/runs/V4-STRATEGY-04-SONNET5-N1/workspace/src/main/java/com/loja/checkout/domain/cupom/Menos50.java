package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(SUBTOTAL_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return DESCONTO;
    }
}
