package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemCarrinhoDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinhoDto> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinhoDto> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinhoDto item : itens) {
            int gratis = item.quantidade() / 3;
            if (gratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
