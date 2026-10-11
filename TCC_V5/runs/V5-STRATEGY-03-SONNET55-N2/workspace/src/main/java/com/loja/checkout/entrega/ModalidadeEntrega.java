package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Opcao;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

public interface ModalidadeEntrega extends Opcao {

    boolean atende(Pedido pedido);

    BigDecimal frete(Pedido pedido);

    int prazoDias(Pedido pedido);
}
