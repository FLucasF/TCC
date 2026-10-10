package com.loja.checkout.entrega;

import com.loja.checkout.catalogo.Identificado;
import java.math.BigDecimal;

/**
 * Uma forma de o cliente receber o pedido. Cada modalidade tem seu jeito de
 * cobrar, seu prazo e suas limitacoes; uma parceria nova e uma classe nova.
 */
public interface ModalidadeEntrega extends Identificado {

    /** Quanto a modalidade cobra para um pedido com este peso. */
    BigDecimal custo(BigDecimal pesoKg);

    int prazoDias();

    /** Se a modalidade atende um pedido com este peso. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
