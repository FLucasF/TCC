package com.loja.checkout.calculator;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.enums.Cupom;
import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;
import java.util.List;

public class CupomCalculator {

    public static class Resultado {
        public BigDecimal desconto;
        public BigDecimal freteAjustado;

        public Resultado(BigDecimal desconto, BigDecimal freteAjustado) {
            this.desconto = desconto;
            this.freteAjustado = freteAjustado;
        }
    }

    public Resultado calcular(Cupom cupom, BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        if (cupom == null) {
            return new Resultado(BigDecimal.ZERO, frete);
        }

        return switch (cupom) {
            case BEMVINDO10 -> desconto10Porcento(subtotal);
            case MENOS50 -> descontoMenos50(subtotal);
            case FRETEGRATIS -> descontoFrteGratis(frete);
            case LEVE3PAGUE2 -> descontoLeve3Pague2(itens);
        };
    }

    private Resultado desconto10Porcento(BigDecimal subtotal) {
        BigDecimal desconto = subtotal.multiply(new BigDecimal("0.10"));
        return new Resultado(Arredondamento.paraCentavos(desconto), new BigDecimal("-1"));
    }

    private Resultado descontoMenos50(BigDecimal subtotal) {
        return new Resultado(new BigDecimal("50.00"), new BigDecimal("-1"));
    }

    private Resultado descontoFrteGratis(BigDecimal frete) {
        return new Resultado(frete, BigDecimal.ZERO);
    }

    private Resultado descontoLeve3Pague2(List<ItemRequest> itens) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            if (item.quantidade() >= 3) {
                int quantidadeGratis = item.quantidade() / 3;
                BigDecimal descontoItem = item.precoUnitario().multiply(new BigDecimal(quantidadeGratis));
                desconto = desconto.add(descontoItem);
            }
        }

        return new Resultado(Arredondamento.paraCentavos(desconto), new BigDecimal("-1"));
    }
}
