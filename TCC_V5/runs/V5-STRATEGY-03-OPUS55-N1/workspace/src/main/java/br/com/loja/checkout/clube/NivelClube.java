package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Codificado;
import java.math.BigDecimal;

/** Um nível do clube e o conjunto de vantagens que ele dá. */
public interface NivelClube extends Codificado {

    BigDecimal credito(BigDecimal subtotalProdutos);

    BigDecimal frete(BigDecimal freteDaModalidade);

    boolean brinde(BigDecimal subtotalProdutos);
}
