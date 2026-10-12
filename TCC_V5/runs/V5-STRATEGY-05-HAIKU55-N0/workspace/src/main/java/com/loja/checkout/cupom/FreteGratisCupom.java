package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.loja.checkout.pedido.Item;

@Component
public class FreteGratisCupom implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return frete;
    }
}
