package com.loja.pedidos.acoes;

import com.loja.pedidos.dominio.Pedido;

/** Algo a fazer quando um pedido é cancelado. {@code estavaPago} diz se há o que desfazer. */
public interface AcaoCancelamento {

    void executar(Pedido pedido, boolean estavaPago);
}
