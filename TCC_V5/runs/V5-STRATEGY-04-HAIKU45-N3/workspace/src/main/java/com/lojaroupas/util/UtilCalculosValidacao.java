package com.lojaroupas.util;

import com.lojaroupas.dto.ItemCarrinho;
import com.lojaroupas.dto.RequisicaoCheckout;
import java.util.List;

public class UtilCalculosValidacao {
    public static Double calcularSubtotal(List<ItemCarrinho> itens) {
        return itens.stream()
            .mapToDouble(ItemCarrinho::getValorTotal)
            .sum();
    }

    public static Double calcularPesoTotal(List<ItemCarrinho> itens) {
        return itens.stream()
            .mapToDouble(ItemCarrinho::getPesoTotal)
            .sum();
    }
}
