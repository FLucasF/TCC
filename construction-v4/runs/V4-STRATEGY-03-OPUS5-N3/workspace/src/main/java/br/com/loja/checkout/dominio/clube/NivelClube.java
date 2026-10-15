package br.com.loja.checkout.dominio.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Identificavel;

/**
 * Nivel do cliente no clube da loja. Cada nivel tem seu conjunto de vantagens
 * numa classe so dele; nivel novo e uma classe nova aqui.
 */
public interface NivelClube extends Identificavel {

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    Dinheiro credito(Dinheiro subtotalProdutos);

    /** O frete que este nivel paga, a partir do frete da entrega escolhida. */
    Dinheiro frete(Dinheiro freteDaEntrega);

    boolean temBrinde(Dinheiro subtotalProdutos);
}
