package com.loja.service;

import com.loja.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public class CalculadorPeso {

    public static BigDecimal calcular(List<ItemRequest> itens) {
        BigDecimal pesoTotal = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            BigDecimal pesoItem = item.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }

        return pesoTotal;
    }
}
