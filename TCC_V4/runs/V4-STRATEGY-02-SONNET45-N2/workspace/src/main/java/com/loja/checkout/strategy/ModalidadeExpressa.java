package com.loja.checkout.strategy;

import com.loja.checkout.dto.ItemCarrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class ModalidadeExpressa implements ModalidadeEntrega {

    @Override
    public String getCodigo() {
        return "EXPRESSA";
    }

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal valorVariavel = new BigDecimal("4.50").multiply(pesoTotal);
        BigDecimal freteTotal = new BigDecimal("25.00").add(valorVariavel);
        return freteTotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean aceitaPedido(List<ItemCarrinho> itens) {
        return true;
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
