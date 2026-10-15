package com.loja.domain.modalidade;

import com.loja.model.ItemCarrinho;
import com.loja.util.Dinheiro;
import java.math.BigDecimal;
import java.util.List;

public class Expressa implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal frete = new BigDecimal("25.00")
            .add(new BigDecimal("4.50").multiply(pesoTotal));

        return Dinheiro.arredondar(frete);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean isDisponivel(List<ItemCarrinho> itens) {
        return true;
    }
}
