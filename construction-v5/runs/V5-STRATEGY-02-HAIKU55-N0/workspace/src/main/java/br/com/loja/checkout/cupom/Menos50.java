package br.com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import br.com.loja.checkout.dominio.ItemCompra;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal VALOR = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(BigDecimal subtotalProdutos, List<ItemCompra> itens, BigDecimal frete) {
        return VALOR;
    }
}
