package com.loja.resumo.cupom;

import com.loja.resumo.model.ItemPedido;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return DESCONTO;
    }
}
