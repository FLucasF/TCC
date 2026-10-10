package com.loja.domain.modalidade;

import com.loja.model.ItemCarrinho;
import com.loja.util.Dinheiro;
import java.math.BigDecimal;
import java.util.List;

public class Economica implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = itens.stream()
            .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal frete = new BigDecimal("12.00")
            .add(new BigDecimal("2.00").multiply(pesoTotal));

        return Dinheiro.arredondar(frete);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean isDisponivel(List<ItemCarrinho> itens) {
        return true;
    }
}
