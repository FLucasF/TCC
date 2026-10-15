package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Codificavel;

/**
 * Uma opcao de entrega. Cada modalidade tem seu jeito de cobrar, seu prazo e
 * suas limitacoes; uma modalidade nova e uma classe nova, e nada mais.
 */
public interface ModalidadeEntrega extends Codificavel {

    /** Se a modalidade atende este pedido (ex.: motoboy so ate 5 kg). */
    boolean atende(Carrinho carrinho);

    Entrega calcular(Carrinho carrinho);
}
