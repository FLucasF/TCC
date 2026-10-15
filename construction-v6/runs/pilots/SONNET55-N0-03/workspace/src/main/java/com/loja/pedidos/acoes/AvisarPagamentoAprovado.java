package com.loja.pedidos.acoes;

import java.util.List;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.TipoAviso;

@Component
public class AvisarPagamentoAprovado implements AcaoPagamentoConfirmado {

    private final List<Notificador> notificadores;

    public AvisarPagamentoAprovado(List<Notificador> notificadores) {
        this.notificadores = notificadores;
    }

    @Override
    public void executar(Pedido pedido) {
        notificadores.forEach(n -> n.notificar(TipoAviso.PAGAMENTO_APROVADO, pedido));
    }
}
