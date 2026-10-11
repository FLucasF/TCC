package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/** Um nível do clube e suas vantagens. Cada nível novo entra como uma nova implementação. */
public interface NivelClube {

    String codigo();

    /** Crédito para a próxima compra, já arredondado para centavos. */
    BigDecimal credito(Carrinho carrinho);

    /** Frete que o cliente deste nível paga, a partir do frete da entrega. */
    BigDecimal frete(BigDecimal freteEntrega);

    boolean brinde(Carrinho carrinho);
}
