package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Codificado;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Cada nivel novo entra como uma implementacao,
 * com seu conjunto de vantagens; por padrao, um nivel nao da vantagem nenhuma.
 */
public interface NivelClube extends Codificado {

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    /** O frete que o cliente deste nivel paga, dado o frete da modalidade escolhida. */
    default BigDecimal frete(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
