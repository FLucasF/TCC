package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            if (unidadesGratis > 0) {
                desconto = desconto.add(
                        item.precoUnitario().multiply(new BigDecimal(unidadesGratis))
                );
            }
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
