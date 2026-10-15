package com.loja.pedidos.acoes;

import java.util.List;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.TipoAviso;

@Component
public class AvisarCancelamento implements AcaoCancelamento {

    private final List<Notificador> notificadores;

    public AvisarCancelamento(List<Notificador> notificadores) {
        this.notificadores = notificadores;
    }

    @Override
    public void executar(Pedido pedido, boolean estavaPago) {
        notificadores.forEach(n -> n.notificar(TipoAviso.PEDIDO_CANCELADO, pedido));
    }
}
