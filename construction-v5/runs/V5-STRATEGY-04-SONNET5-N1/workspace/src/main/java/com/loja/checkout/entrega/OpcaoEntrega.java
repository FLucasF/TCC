package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Cada opção tem seu próprio jeito de cobrar, seu
 * próprio prazo e suas próprias limitações de disponibilidade — uma nova
 * transportadora é adicionada implementando esta interface, sem tocar nas
 * demais.
 */
public interface OpcaoEntrega {

    String codigo();

    int prazoDias();

    boolean disponivel(Pedido pedido);

    BigDecimal custo(Pedido pedido);
}
