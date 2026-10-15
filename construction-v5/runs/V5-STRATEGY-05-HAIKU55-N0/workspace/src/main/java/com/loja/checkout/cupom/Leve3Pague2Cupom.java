package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.loja.checkout.calculo.Dinheiro;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.ErroCheckout;
import com.loja.checkout.pedido.Item;

@Component
public class Leve3Pague2Cupom implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        if (desconto.signum() == 0) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(desconto);
    }
}
