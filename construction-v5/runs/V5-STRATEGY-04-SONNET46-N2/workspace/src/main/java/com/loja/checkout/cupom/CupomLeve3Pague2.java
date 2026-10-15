package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
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
    public void verificarAplicabilidade(BigDecimal subtotalProdutos, BigDecimal frete) {
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int unidadesGratis = Math.floorDiv(item.quantidade(), 3);
            total = total.add(BigDecimal.valueOf(unidadesGratis).multiply(item.precoUnitario()));
        }
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }
}
