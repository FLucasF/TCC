package br.com.loja.pedidos.cliente;

import br.com.loja.pedidos.pedido.eventos.PagamentoAprovado;
import br.com.loja.pedidos.pedido.eventos.PedidoCancelado;
import java.math.BigDecimal;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** O crédito do clube entra no saldo quando o pagamento é confirmado e sai se o pedido pago for cancelado. */
@Component
public class CreditoDoClube {

    private final ClienteRepositorio clientes;

    public CreditoDoClube(ClienteRepositorio clientes) {
        this.clientes = clientes;
    }

    @EventListener
    public void aoAprovarPagamento(PagamentoAprovado evento) {
        BigDecimal credito = evento.pedido().resumo().creditoProximaCompra();
        if (credito.signum() != 0) {
            clientes.movimentarCredito(evento.pedido().cpf(), credito);
        }
    }

    @EventListener
    public void aoCancelar(PedidoCancelado evento) {
        BigDecimal credito = evento.pedido().resumo().creditoProximaCompra();
        if (evento.estornarPagamento() && credito.signum() != 0) {
            clientes.movimentarCredito(evento.pedido().cpf(), credito.negate());
        }
    }
}
