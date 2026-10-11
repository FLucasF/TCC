package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Codificado;
import java.math.BigDecimal;

public interface NivelClube extends Codificado {

    /** Frete que o cliente paga, a partir do frete da modalidade. */
    BigDecimal frete(BigDecimal freteModalidade);

    BigDecimal credito(Carrinho carrinho);

    boolean brinde(Carrinho carrinho);
}
