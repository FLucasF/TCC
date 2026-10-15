package com.loja.dominio;

import java.util.UUID;

import org.springframework.stereotype.Component;

/** Registra o aviso na lista que o atendimento consulta. */
@Component
public class CanalAvisos implements CanalNotificacao {

    @Override
    public void notificar(Estado estado, Mudancas mudancas, Evento evento) {
        Aviso aviso = new Aviso(UUID.randomUUID().toString(), estado.proximaSequenciaAviso(),
                evento.cpf(), evento.tipo(), evento.pedidoId(), false);
        estado.adicionarAviso(aviso);
        mudancas.avisos.add(aviso);
    }
}
