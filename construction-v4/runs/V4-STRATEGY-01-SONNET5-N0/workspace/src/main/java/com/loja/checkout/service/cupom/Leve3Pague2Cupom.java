package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemPedidoRequest;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Leve3Pague2Cupom implements CalculadoraCupom {

    private static final int QUANTIDADE_POR_GRUPO = 3;

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemPedidoRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = Dinheiro.zero();
        for (ItemPedidoRequest item : itens) {
            int itensGratis = item.quantidade() / QUANTIDADE_POR_GRUPO;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(itensGratis)));
        }
        return desconto;
    }
}
