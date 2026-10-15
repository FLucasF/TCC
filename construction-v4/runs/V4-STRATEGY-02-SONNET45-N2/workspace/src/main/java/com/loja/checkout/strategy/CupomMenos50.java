package com.loja.checkout.strategy;

import com.loja.checkout.dto.ItemCarrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return subtotalProdutos.compareTo(VALOR_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return DESCONTO;
    }
}
