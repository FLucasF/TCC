package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.ContextoPedido;
import com.loja.checkout.dominio.ResultadoEntrega;
import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {

    @Override
    public boolean estaDisponivel(ContextoPedido contexto) {
        return contexto.getPesoTotal().compareTo(new BigDecimal("5.00")) <= 0;
    }

    @Override
    public ResultadoEntrega calcular(ContextoPedido contexto) {
        return new ResultadoEntrega(new BigDecimal("18.00"), 0);
    }
}
