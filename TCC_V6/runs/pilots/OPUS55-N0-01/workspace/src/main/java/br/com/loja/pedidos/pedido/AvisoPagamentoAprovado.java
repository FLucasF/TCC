package br.com.loja.pedidos.pedido;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.com.loja.pedidos.aviso.AvisoRepositorio;
import br.com.loja.pedidos.aviso.TipoAviso;

@Component
@Order(2)
public class AvisoPagamentoAprovado implements EfeitoPagamento {

    private final AvisoRepositorio avisos;

    public AvisoPagamentoAprovado(AvisoRepositorio avisos) {
        this.avisos = avisos;
    }

    @Override
    public void aoConfirmar(Pedido pedido) {
        avisos.registrar(pedido.cpf(), TipoAviso.PAGAMENTO_APROVADO, pedido.id());
    }
}
