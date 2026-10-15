package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.comum.ComCodigo;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. O enunciado avisa que quase toda semana entra uma
 * opcao nova, cada uma com seu jeito de cobrar, seu prazo e suas limitacoes:
 * por isso cada modalidade mora na sua propria classe. A assinatura atende a
 * mais exigente -- a que limita o pedido (motoboy) e a que cobra por peso.
 */
public interface ModalidadeEntrega extends ComCodigo {

    /** Se a modalidade atende um pedido com este peso. Por padrao atende. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }

    /** Frete bruto para este peso; o servico arredonda para centavos. */
    BigDecimal frete(BigDecimal pesoKg);

    int prazoDias();
}
