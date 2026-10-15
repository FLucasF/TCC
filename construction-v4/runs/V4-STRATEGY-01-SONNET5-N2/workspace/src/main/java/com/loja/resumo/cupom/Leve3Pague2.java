package com.loja.resumo.cupom;

import com.loja.resumo.model.ItemPedido;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {

    private static final int TAMANHO_LOTE = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            int unidadesGratis = item.quantidade() / TAMANHO_LOTE;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return desconto;
    }
}
