package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.ContextoPedido;
import com.loja.checkout.dominio.ResultadoEntrega;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {

    @Override
    public boolean estaDisponivel(ContextoPedido contexto) {
        return true;
    }

    @Override
    public ResultadoEntrega calcular(ContextoPedido contexto) {
        BigDecimal frete = new BigDecimal("12.00")
            .add(contexto.getPesoTotal().multiply(new BigDecimal("2.00")));
        return new ResultadoEntrega(Arredondamento.arredondar(frete), 7);
    }
}
