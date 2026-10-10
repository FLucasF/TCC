package com.loja.resumo.cupom;

import com.loja.resumo.model.ItemPedido;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return subtotalProdutos.multiply(PERCENTUAL);
    }
}
