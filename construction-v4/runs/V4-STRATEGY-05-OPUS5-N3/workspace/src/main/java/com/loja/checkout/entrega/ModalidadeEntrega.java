package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Registro;
import java.math.BigDecimal;

/**
 * Uma forma de o cliente receber o pedido. Cada parceria tem seu jeito de
 * cobrar, seu prazo e suas limitacoes, e cada uma mora na sua propria classe.
 */
public interface ModalidadeEntrega extends Registro.Identificado {

    /** Se a modalidade atende um pedido com esse peso. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }

    /** Quanto a modalidade cobra de frete por um pedido com esse peso. */
    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();
}
