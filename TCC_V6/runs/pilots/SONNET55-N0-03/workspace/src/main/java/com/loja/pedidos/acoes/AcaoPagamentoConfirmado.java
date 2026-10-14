package com.loja.pedidos.acoes;

import com.loja.pedidos.dominio.Pedido;

/** Algo a fazer quando o pagamento é confirmado (o estoque é tratado à parte). Nova ação = novo bean. */
public interface AcaoPagamentoConfirmado {

    void executar(Pedido pedido);
}
