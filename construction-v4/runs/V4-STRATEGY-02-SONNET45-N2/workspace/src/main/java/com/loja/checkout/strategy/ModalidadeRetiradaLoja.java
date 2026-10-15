package com.loja.checkout.strategy;

import com.loja.checkout.dto.ItemCarrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ModalidadeRetiradaLoja implements ModalidadeEntrega {

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public Integer getPrazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean aceitaPedido(List<ItemCarrinho> itens) {
        return true;
    }
}
