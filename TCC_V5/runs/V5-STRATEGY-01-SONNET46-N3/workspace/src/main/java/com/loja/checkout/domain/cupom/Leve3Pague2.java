package com.loja.checkout.domain.cupom;

import com.loja.checkout.web.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() { return "LEVE3PAGUE2"; }

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) { return true; }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        BigDecimal total = new BigDecimal("0.00");
        for (ItemRequest item : itens) {
            int gratis = item.quantidade() / 3;
            if (gratis > 0) {
                BigDecimal descontoItem = item.precoUnitario()
                        .multiply(BigDecimal.valueOf(gratis))
                        .setScale(2, RoundingMode.HALF_EVEN);
                total = total.add(descontoItem);
            }
        }
        return total;
    }
}
