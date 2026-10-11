package com.loja.checkout.domain.modalidade;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Expressa implements ModalidadeEntrega {
    @Override
    public boolean aceitaPedido(List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        double pesoTotal = itens.stream()
            .mapToDouble(item -> item.pesoKg() * item.quantidade())
            .sum();

        BigDecimal frete = new BigDecimal("25.00")
            .add(new BigDecimal("4.50").multiply(BigDecimal.valueOf(pesoTotal)));

        return frete.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int obterPrazoDias() {
        return 2;
    }
}
