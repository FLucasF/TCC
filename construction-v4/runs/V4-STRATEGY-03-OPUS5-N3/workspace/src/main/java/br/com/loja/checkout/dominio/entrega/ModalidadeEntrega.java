package br.com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Identificavel;

/**
 * Uma opcao de entrega. Cada parceria tem seu jeito de cobrar, seu prazo e suas
 * limitacoes, e tudo isso mora na classe da propria opcao: entrar uma
 * transportadora nova e criar uma classe aqui, sem mexer em nada mais.
 */
public interface ModalidadeEntrega extends Identificavel {

    /** Se esta opcao atende um pedido com este peso. */
    boolean atende(BigDecimal pesoKg);

    Dinheiro frete(BigDecimal pesoKg);

    int prazoDias();
}
