package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.ContextoPedido;
import com.loja.checkout.dominio.ResultadoEntrega;
import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public boolean estaDisponivel(ContextoPedido contexto) {
        return true;
    }

    @Override
    public ResultadoEntrega calcular(ContextoPedido contexto) {
        return new ResultadoEntrega(new BigDecimal("0.00"), 1);
    }
}
