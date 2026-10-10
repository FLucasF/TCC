package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            int gratuitos = item.quantidade() / 3;
            if (gratuitos > 0) {
                desconto = desconto.add(
                        item.precoUnitario().multiply(new BigDecimal(gratuitos))
                );
            }
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
