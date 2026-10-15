package com.loja.checkout.entrega;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaHandler implements ModalidadeEntregaHandler {

    @Override
    public ModalidadeEntrega getModalidade() {
        return ModalidadeEntrega.RETIRADA_LOJA;
    }

    @Override
    public boolean disponivel(BigDecimal pesoPedidoKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoPedidoKg) {
        return Dinheiro.arredondar(BigDecimal.ZERO);
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }
}
