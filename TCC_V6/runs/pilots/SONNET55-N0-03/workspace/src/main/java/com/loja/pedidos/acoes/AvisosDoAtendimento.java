package com.loja.pedidos.acoes;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.TipoAviso;
import com.loja.pedidos.persistencia.AvisoRepositorio;

/** Canal atual: registra o aviso numa lista que o atendimento consulta. */
@Component
public class AvisosDoAtendimento implements Notificador {

    private final AvisoRepositorio avisos;

    public AvisosDoAtendimento(AvisoRepositorio avisos) {
        this.avisos = avisos;
    }

    @Override
    public void notificar(TipoAviso tipo, Pedido pedido) {
        avisos.inserir(pedido.cpf(), tipo, pedido.id());
    }
}
