package br.com.loja.checkout.clube;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Codificado;
import java.math.BigDecimal;

/** Um nível do clube com suas vantagens. Para incluir um novo, basta criar outro componente que implemente esta interface. */
public interface NivelClube extends Codificado {

    BigDecimal creditoProximaCompra(Carrinho carrinho);

    boolean isentaFrete();

    boolean brinde(Carrinho carrinho);
}
