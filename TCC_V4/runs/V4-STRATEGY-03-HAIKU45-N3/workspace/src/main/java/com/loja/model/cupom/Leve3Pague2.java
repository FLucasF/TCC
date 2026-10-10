package com.loja.model.cupom;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Leve3Pague2 implements CalculadoraDesconto {
    @Override
    public BigDecimal calcular(BigDecimal subtotal, List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        Map<String, Integer> quantidadePorItem = new HashMap<>();
        Map<String, BigDecimal> precoPorItem = new HashMap<>();

        for (ItemCarrinho item : itens) {
            quantidadePorItem.merge(item.getNome(), item.getQuantidade(), Integer::sum);
            precoPorItem.putIfAbsent(item.getNome(), item.getPrecoUnitario());
        }

        for (String nome : quantidadePorItem.keySet()) {
            int totalQuantidade = quantidadePorItem.get(nome);
            int unidadeGratis = totalQuantidade / 3;
            BigDecimal preco = precoPorItem.get(nome);
            desconto = desconto.add(preco.multiply(new BigDecimal(unidadeGratis)));
        }

        return desconto;
    }

    @Override
    public boolean ehFretegratis() {
        return false;
    }
}
