package br.com.loja.pedidos.aviso;

import br.com.loja.pedidos.pedido.Pedido;
import br.com.loja.pedidos.pedido.eventos.PagamentoAprovado;
import br.com.loja.pedidos.pedido.eventos.PedidoCancelado;
import br.com.loja.pedidos.pedido.eventos.PedidoEnviado;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Por enquanto, avisar o cliente é registrar a mensagem na lista que o atendimento consulta.
 * O envio de e-mail ou SMS pode ser outro ouvinte destes mesmos eventos.
 */
@Component
public class AvisosAoCliente {

    private final AvisoRepositorio avisos;
    private final Clock relogio;

    public AvisosAoCliente(AvisoRepositorio avisos, Clock relogio) {
        this.avisos = avisos;
        this.relogio = relogio;
    }

    @EventListener
    public void aoAprovarPagamento(PagamentoAprovado evento) {
        avisar(evento.pedido(), TipoAviso.PAGAMENTO_APROVADO);
    }

    @EventListener
    public void aoEnviar(PedidoEnviado evento) {
        avisar(evento.pedido(), TipoAviso.PEDIDO_ENVIADO);
    }

    @EventListener
    public void aoCancelar(PedidoCancelado evento) {
        avisar(evento.pedido(), TipoAviso.PEDIDO_CANCELADO);
    }

    private void avisar(Pedido pedido, TipoAviso tipo) {
        avisos.registrar(pedido.cpf(), pedido.id(), tipo, relogio.instant());
    }
}
