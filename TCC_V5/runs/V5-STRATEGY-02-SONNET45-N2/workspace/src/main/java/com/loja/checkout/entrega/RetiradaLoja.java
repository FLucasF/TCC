package com.loja.checkout.entrega;

import com.loja.checkout.model.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("RETIRADA_LOJA")
public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public Integer prazoEntregaDias() {
        return 1;
    }

    @Override
    public void validarDisponibilidade(Pedido pedido) {
    }
}
