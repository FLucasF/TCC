package com.loja.dominio;

/** Um meio de avisar o cliente. Para criar um novo (e-mail, SMS...), basta uma nova classe @Component. */
public interface CanalNotificacao {

    record Evento(String cpf, TipoAviso tipo, String pedidoId) {
    }

    void notificar(Estado estado, Mudancas mudancas, Evento evento);
}
