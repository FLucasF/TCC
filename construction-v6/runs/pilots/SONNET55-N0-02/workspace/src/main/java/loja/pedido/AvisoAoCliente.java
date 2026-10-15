package loja.pedido;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import loja.dominio.Aviso;
import loja.dominio.Pedido;
import loja.infra.AvisoRepository;

/** Por enquanto o aviso fica numa lista consultada pelo atendimento; o e-mail de verdade vem depois. */
@Component
@Order(3)
public class AvisoAoCliente implements AcaoPedido {

    private final AvisoRepository avisos;

    public AvisoAoCliente(AvisoRepository avisos) {
        this.avisos = avisos;
    }

    @Override
    public void aoPagar(Pedido p) {
        avisos.criar(p.cpf(), Aviso.PAGAMENTO_APROVADO, p.id(),
                "Pagamento aprovado para o pedido " + p.id() + " (" + p.email() + ").");
    }

    @Override
    public void aoCancelar(Pedido p, boolean estavaPago) {
        avisos.criar(p.cpf(), Aviso.PEDIDO_CANCELADO, p.id(),
                "Pedido " + p.id() + " cancelado (" + p.email() + ").");
    }
}
