package com.loja.checkout.service;

import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.Item;
import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;
import java.util.List;

public class CalculadorCupom {

    public BigDecimal calcular(String codigoCupom, List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        if (codigoCupom == null) {
            return BigDecimal.ZERO;
        }

        Cupom cupom;
        try {
            cupom = Cupom.valueOf(codigoCupom);
        } catch (IllegalArgumentException e) {
            return null;
        }

        return switch (cupom) {
            case BEMVINDO10 -> calcularBemvindo10(subtotalProdutos);
            case MENOS50 -> calcularMenos50(subtotalProdutos);
            case FRETEGRATIS -> frete;
            case LEVE3PAGUE2 -> calcularLeve3Pague2(itens, subtotalProdutos);
        };
    }

    private BigDecimal calcularBemvindo10(BigDecimal subtotalProdutos) {
        BigDecimal desconto = subtotalProdutos.multiply(Arredondador.arredondar(0.10));
        return Arredondador.arredondar(desconto);
    }

    private BigDecimal calcularMenos50(BigDecimal subtotalProdutos) {
        if (subtotalProdutos.compareTo(Arredondador.arredondar(300.0)) < 0) {
            return null;
        }
        return Arredondador.arredondar(50.0);
    }

    private BigDecimal calcularLeve3Pague2(List<Item> itens, BigDecimal subtotalProdutos) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (Item item : itens) {
            int quantidade = item.getQuantidade();
            int gratuitas = quantidade / 3;

            if (gratuitas > 0) {
                BigDecimal descutoItem = Arredondador.arredondar(item.getPrecoUnitario())
                    .multiply(new BigDecimal(gratuitas));
                desconto = desconto.add(descutoItem);
            }
        }

        return Arredondador.arredondar(desconto);
    }
}
