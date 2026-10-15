package com.loja.checkout.entrega;

import com.loja.checkout.dominio.ModalidadeEntrega;
import java.math.BigDecimal;

/**
 * Cada modalidade de entrega tem seu jeito de cobrar, seu prazo e suas
 * proprias limitacoes. Como a loja fecha parceria com transportadoras novas
 * o tempo todo, uma modalidade nova so precisa de uma implementacao nova
 * desta interface, sem tocar em quem escolhe entre elas.
 */
public interface OpcaoEntrega {

    ModalidadeEntrega modalidade();

    boolean disponivel(BigDecimal pesoTotalKg);

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoDias();
}
