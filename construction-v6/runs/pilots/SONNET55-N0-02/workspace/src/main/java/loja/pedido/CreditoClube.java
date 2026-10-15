package loja.pedido;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import loja.dominio.Pedido;
import loja.infra.ClienteRepository;

@Component
@Order(2)
public class CreditoClube implements AcaoPedido {

    private final ClienteRepository clientes;

    public CreditoClube(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Override
    public void aoPagar(Pedido pedido) {
        clientes.somarCredito(pedido.cpf(), pedido.resumo().creditoProximaCompra());
    }

    @Override
    public void aoCancelar(Pedido pedido, boolean estavaPago) {
        if (estavaPago) {
            clientes.somarCredito(pedido.cpf(), pedido.resumo().creditoProximaCompra().negate());
        }
    }
}
