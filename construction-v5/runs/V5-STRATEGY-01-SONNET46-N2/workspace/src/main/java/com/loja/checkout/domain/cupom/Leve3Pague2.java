package com.loja.checkout.domain.cupom;

import com.loja.checkout.web.dto.ItemDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Leve3Pague2 implements Cupom {

    @Override
    public void validarAplicabilidade(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        // sem restrição
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemDto> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            if (unidadesGratis > 0) {
                desconto = desconto.add(
                        item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis))
                );
            }
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
