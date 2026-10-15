package com.loja.pedidos.acoes;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.persistencia.ClienteRepositorio;

@Component
public class CreditarClube implements AcaoPagamentoConfirmado, AcaoCancelamento {

    private final ClienteRepositorio clientes;

    public CreditarClube(ClienteRepositorio clientes) {
        this.clientes = clientes;
    }

    @Override
    public void executar(Pedido pedido) {
        clientes.ajustarSaldo(pedido.cpf(), pedido.resumo().creditoProximaCompra());
    }

    @Override
    public void executar(Pedido pedido, boolean estavaPago) {
        if (estavaPago) {
            clientes.ajustarSaldo(pedido.cpf(), pedido.resumo().creditoProximaCompra().negate());
        }
    }
}
