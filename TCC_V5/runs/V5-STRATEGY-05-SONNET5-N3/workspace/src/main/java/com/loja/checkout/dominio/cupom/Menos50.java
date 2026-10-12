package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal freteExibido) {
        return DESCONTO;
    }
}
