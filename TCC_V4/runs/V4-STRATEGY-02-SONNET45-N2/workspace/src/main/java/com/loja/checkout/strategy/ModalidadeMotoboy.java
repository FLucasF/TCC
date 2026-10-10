package com.loja.checkout.strategy;

import com.loja.checkout.dto.ItemCarrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ModalidadeMotoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return new BigDecimal("18.00");
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean aceitaPedido(List<ItemCarrinho> itens) {
        BigDecimal pesoTotal = calcularPesoTotal(itens);
        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }

    private BigDecimal calcularPesoTotal(List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
