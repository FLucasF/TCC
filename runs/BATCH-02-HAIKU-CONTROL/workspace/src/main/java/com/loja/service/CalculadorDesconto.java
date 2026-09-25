package com.loja.service;

import com.loja.dto.ItemRequest;
import com.loja.enums.Cupom;
import com.loja.util.ArredondadorMeioParaPar;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CalculadorDesconto {

    public static BigDecimal calcular(String cupomStr, BigDecimal subtotal, BigDecimal frete, List<ItemRequest> itens) {
        if (cupomStr == null) {
            return ArredondadorMeioParaPar.arredondar(BigDecimal.ZERO);
        }

        Cupom cupom = Cupom.fromString(cupomStr);
        if (cupom == null) {
            return ArredondadorMeioParaPar.arredondar(BigDecimal.ZERO);
        }

        return switch (cupom) {
            case BEMVINDO10 -> aplicarBemVindo10(subtotal);
            case MENOS50 -> aplicarMenos50(subtotal);
            case FRETEGRATIS -> aplicarFreteGratis(frete);
            case LEVE3PAGUE2 -> aplicarLeve3Pague2(itens);
        };
    }

    private static BigDecimal aplicarBemVindo10(BigDecimal subtotal) {
        BigDecimal desconto = subtotal.multiply(new BigDecimal("0.10"));
        return ArredondadorMeioParaPar.arredondar(desconto);
    }

    private static BigDecimal aplicarMenos50(BigDecimal subtotal) {
        return ArredondadorMeioParaPar.arredondar(new BigDecimal("50.00"));
    }

    private static BigDecimal aplicarFreteGratis(BigDecimal frete) {
        return frete;
    }

    private static BigDecimal aplicarLeve3Pague2(List<ItemRequest> itens) {
        Map<String, Integer> quantidadesPorItem = itens.stream()
            .collect(Collectors.toMap(
                ItemRequest::getNome,
                ItemRequest::getQuantidade,
                Integer::sum
            ));

        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            int quantidade = quantidadesPorItem.get(item.getNome());
            int gratuitos = quantidade / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(gratuitos));
            desconto = desconto.add(descontoItem);
        }

        return ArredondadorMeioParaPar.arredondar(desconto);
    }
}
