package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemCarrinhoDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomBemVindo10 implements Cupom {

    private static final BigDecimal DEZ_PORCENTO = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinhoDto> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinhoDto> itens) {
        return subtotalProdutos.multiply(DEZ_PORCENTO).setScale(2, RoundingMode.HALF_EVEN);
    }
}
