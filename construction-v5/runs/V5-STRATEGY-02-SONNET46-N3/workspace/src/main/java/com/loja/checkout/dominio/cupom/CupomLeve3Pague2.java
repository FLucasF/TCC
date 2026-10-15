package com.loja.checkout.dominio.cupom;

import com.loja.checkout.api.ResumoRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {

    @Override
    public boolean eAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ResumoRequest.Item> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ResumoRequest.Item item : itens) {
            int gratis = item.quantidade() / 3;
            if (gratis > 0) {
                total = total.add(item.precoUnitario().multiply(new BigDecimal(gratis)));
            }
        }
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }
}
