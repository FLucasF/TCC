package com.loja.checkout.service;

import com.loja.checkout.model.Item;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;
import java.util.List;

public class CalculadorFrete {

    public record ResultadoFrete(BigDecimal valor, Integer prazo) {}

    public ResultadoFrete calcular(List<Item> itens, ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> calcularEconomica(itens);
            case EXPRESSA -> calcularExpressa(itens);
            case RETIRADA_LOJA -> new ResultadoFrete(BigDecimal.ZERO, 1);
            case MOTOBOY -> new ResultadoFrete(new BigDecimal("18.00"), 0);
        };
    }

    private ResultadoFrete calcularEconomica(List<Item> itens) {
        BigDecimal peso = calcularPesoTotal(itens);
        BigDecimal frete = new BigDecimal("12.00").add(peso.multiply(new BigDecimal("2.00")));
        frete = Arredondador.arredondar(frete);
        return new ResultadoFrete(frete, 7);
    }

    private ResultadoFrete calcularExpressa(List<Item> itens) {
        BigDecimal peso = calcularPesoTotal(itens);
        BigDecimal frete = new BigDecimal("25.00").add(peso.multiply(new BigDecimal("4.50")));
        frete = Arredondador.arredondar(frete);
        return new ResultadoFrete(frete, 2);
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        return itens.stream()
            .map(item -> new BigDecimal(item.getPesoKg()).multiply(new BigDecimal(item.getQuantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
