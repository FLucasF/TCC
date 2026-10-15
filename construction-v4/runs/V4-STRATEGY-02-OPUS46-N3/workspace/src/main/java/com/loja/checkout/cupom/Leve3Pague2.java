package com.loja.checkout.cupom;

import com.loja.checkout.Moeda;
import com.loja.checkout.ResumoRequest.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int gratis = item.quantidade() / 3;
            if (gratis > 0) {
                desconto = desconto.add(
                        item.precoUnitario().multiply(new BigDecimal(gratis)));
            }
        }
        return Moeda.arredondar(desconto);
    }
}
