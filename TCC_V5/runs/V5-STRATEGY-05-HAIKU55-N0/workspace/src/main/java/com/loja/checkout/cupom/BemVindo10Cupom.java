package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.loja.checkout.calculo.Dinheiro;
import com.loja.checkout.pedido.Item;

@Component
public class BemVindo10Cupom implements Cupom {

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return Dinheiro.percentual(subtotal, new BigDecimal("0.10"));
    }
}
