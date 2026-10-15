package com.loja.checkout.strategy;

import com.loja.checkout.dto.ItemCarrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class ModalidadeEconomica implements ModalidadeEntrega {

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        BigDecimal valorVariavel = new BigDecimal("2.00").multiply(pesoTotal);
        BigDecimal freteTotal = new BigDecimal("12.00").add(valorVariavel);
        return freteTotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 7;
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
