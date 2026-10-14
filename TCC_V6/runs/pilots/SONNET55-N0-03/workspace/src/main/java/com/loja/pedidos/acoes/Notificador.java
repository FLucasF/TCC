package com.loja.pedidos.acoes;

import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.TipoAviso;

/** Um canal de aviso ao cliente. Novo canal (e-mail, SMS) = novo bean. */
public interface Notificador {

    void notificar(TipoAviso tipo, Pedido pedido);
}
