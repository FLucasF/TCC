package com.loja.checkout.domain.cupom;

import com.loja.checkout.web.dto.ItemDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Bemvindo10 implements Cupom {

    private static final BigDecimal DEZ_PORCENTO = new BigDecimal("0.10");

    @Override
    public void validarAplicabilidade(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        // sem restrição
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        return subtotal.multiply(DEZ_PORCENTO).setScale(2, RoundingMode.HALF_EVEN);
    }
}
