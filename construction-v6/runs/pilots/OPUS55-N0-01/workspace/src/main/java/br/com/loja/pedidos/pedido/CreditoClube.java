package br.com.loja.pedidos.pedido;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.com.loja.pedidos.cliente.ClienteRepositorio;

/** Coloca no saldo o crédito do clube ao confirmar o pagamento e tira de volta se o pedido pago for cancelado. */
@Component
@Order(1)
public class CreditoClube implements EfeitoPagamento {

    private final ClienteRepositorio clientes;

    public CreditoClube(ClienteRepositorio clientes) {
        this.clientes = clientes;
    }

    @Override
    public void aoConfirmar(Pedido pedido) {
        clientes.alterarSaldo(pedido.cpf(), pedido.resumo().creditoProximaCompra());
    }

    @Override
    public void aoCancelarPedidoPago(Pedido pedido) {
        clientes.alterarSaldo(pedido.cpf(), pedido.resumo().creditoProximaCompra().negate());
    }
}
