package com.loja.checkout.domain.cupom;

import com.loja.checkout.api.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomLeve3Pague2 implements AplicadorCupom {

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int gratuitos = item.quantidade() / 3;
            if (gratuitos > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(new BigDecimal(gratuitos)));
            }
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
