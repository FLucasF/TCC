package com.loja.checkout.domain.modalidade;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class Motoboy implements ModalidadeEntrega {
    private static final double PESO_MAXIMO_KG = 5.0;

    @Override
    public boolean aceitaPedido(List<ItemCarrinho> itens) {
        double pesoTotal = itens.stream()
            .mapToDouble(item -> item.pesoKg() * item.quantidade())
            .sum();
        return pesoTotal <= PESO_MAXIMO_KG;
    }

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return new BigDecimal("18.00");
    }

    @Override
    public int obterPrazoDias() {
        return 0;
    }
}
