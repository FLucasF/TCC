package br.com.loja.checkout.clube;

import br.com.loja.checkout.Codificado;

import java.math.BigDecimal;

/** Um nível do clube e suas vantagens. Para incluir um novo, basta criar outra implementação anotada com @Component. */
public interface NivelClube extends Codificado {

    /** Frete que o cliente deste nível paga, partindo do frete da opção de entrega. */
    BigDecimal frete(BigDecimal freteDaEntrega);

    /** Crédito para a próxima compra, já arredondado para centavos. */
    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    boolean brinde(BigDecimal subtotalProdutos);
}
