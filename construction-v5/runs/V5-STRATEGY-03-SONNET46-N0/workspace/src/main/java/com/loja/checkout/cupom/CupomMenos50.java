package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemCarrinhoDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomMenos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinhoDto> itens) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinhoDto> itens) {
        return DESCONTO.setScale(2, RoundingMode.HALF_EVEN);
    }
}
