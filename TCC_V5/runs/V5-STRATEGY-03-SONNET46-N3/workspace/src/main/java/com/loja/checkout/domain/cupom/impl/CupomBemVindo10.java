package com.loja.checkout.domain.cupom.impl;

import com.loja.checkout.domain.cupom.CupomStrategy;
import com.loja.checkout.dto.ItemDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomBemVindo10 implements CupomStrategy {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
    }
}
