package com.loja.checkout.domain.cupom.impl;

import com.loja.checkout.domain.cupom.CupomStrategy;
import com.loja.checkout.dto.ItemDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomFreteGratis implements CupomStrategy {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        return frete;
    }
}
