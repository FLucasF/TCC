package br.com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.ItemCompra;

@Component
public class BemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public BigDecimal desconto(BigDecimal subtotalProdutos, List<ItemCompra> itens, BigDecimal frete) {
        return subtotalProdutos.multiply(PERCENTUAL);
    }
}
