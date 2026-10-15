package com.loja.checkout.cupom;

import com.loja.checkout.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomMenos50 implements CalculoCupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens, BigDecimal frete) {
        return DESCONTO;
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }
}
