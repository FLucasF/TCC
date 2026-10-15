package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import com.loja.checkout.dto.ItemRequest;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int gratuitos = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(new BigDecimal(gratuitos)));
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
