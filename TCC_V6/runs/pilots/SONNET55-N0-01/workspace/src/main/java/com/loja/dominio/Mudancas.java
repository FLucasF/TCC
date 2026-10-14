package com.loja.dominio;

import java.util.LinkedHashSet;
import java.util.Set;

/** Objetos alterados numa operação, para gravar tudo junto numa única transação. */
public class Mudancas {

    public final Set<Produto> produtos = new LinkedHashSet<>();
    public final Set<Cliente> clientes = new LinkedHashSet<>();
    public final Set<Pedido> pedidos = new LinkedHashSet<>();
    public final Set<Aviso> avisos = new LinkedHashSet<>();

    public boolean vazia() {
        return produtos.isEmpty() && clientes.isEmpty() && pedidos.isEmpty() && avisos.isEmpty();
    }
}
