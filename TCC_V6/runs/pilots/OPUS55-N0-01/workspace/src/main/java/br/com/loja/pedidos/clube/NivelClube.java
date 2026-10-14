package br.com.loja.pedidos.clube;

import java.math.BigDecimal;

import br.com.loja.pedidos.comum.Dinheiro;
import br.com.loja.pedidos.entrega.ModalidadeEntrega;

/** Um nível do clube da loja. Para criar um nível novo, basta criar um componente que implemente esta interface. */
public interface NivelClube {

    String codigo();

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    /** Se o cliente deste nível deixa de pagar o frete nesta opção de entrega. */
    default boolean isentaFrete(ModalidadeEntrega modalidade) {
        return false;
    }

    default boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
