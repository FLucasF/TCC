package com.loja.checkout.dominio.cupom;

import com.loja.checkout.api.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            if (unidadesGratis > 0) {
                BigDecimal itemDesconto = item.precoUnitario()
                        .multiply(BigDecimal.valueOf(unidadesGratis))
                        .setScale(2, RoundingMode.HALF_EVEN);
                desconto = desconto.add(itemDesconto);
            }
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public void validarAplicabilidade(BigDecimal subtotalProdutos) {
        // sem restrição
    }
}
