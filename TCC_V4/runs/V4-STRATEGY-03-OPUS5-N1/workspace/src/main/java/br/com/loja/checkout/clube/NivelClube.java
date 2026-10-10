package br.com.loja.checkout.clube;

import br.com.loja.checkout.catalogo.Codificado;
import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Cada nivel novo entra como uma implementacao
 * desta interface, com seu conjunto de vantagens.
 */
public interface NivelClube extends Codificado {

    /** Credito para a proxima compra, em centavos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean isentaFrete();

    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
