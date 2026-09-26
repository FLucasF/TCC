package com.loja.service;

import com.loja.dto.ItemRequest;
import com.loja.util.ArredondadorMeioParaPar;

import java.math.BigDecimal;
import java.util.List;

public class CalculadorSubtotal {

    public static BigDecimal calcular(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            BigDecimal precoItem = item.getPrecoUnitario().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(precoItem);
        }

        return ArredondadorMeioParaPar.arredondar(subtotal);
    }
}
