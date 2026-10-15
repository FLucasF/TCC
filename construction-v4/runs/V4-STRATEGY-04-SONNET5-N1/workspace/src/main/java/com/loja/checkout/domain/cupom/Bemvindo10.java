package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return subtotalProdutos.multiply(PERCENTUAL);
    }
}
