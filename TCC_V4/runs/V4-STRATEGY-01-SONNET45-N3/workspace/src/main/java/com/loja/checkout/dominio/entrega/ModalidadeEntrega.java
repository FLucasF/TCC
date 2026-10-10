package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.ContextoPedido;
import com.loja.checkout.dominio.ResultadoEntrega;

public interface ModalidadeEntrega {
    boolean estaDisponivel(ContextoPedido contexto);
    ResultadoEntrega calcular(ContextoPedido contexto);
}
