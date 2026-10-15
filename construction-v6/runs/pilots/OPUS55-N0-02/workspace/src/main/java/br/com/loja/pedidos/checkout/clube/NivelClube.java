package br.com.loja.pedidos.checkout.clube;

import br.com.loja.pedidos.checkout.entrega.ModalidadeEntrega;
import br.com.loja.pedidos.comum.Dinheiro;
import java.math.BigDecimal;

/**
 * Um nível do clube da loja e suas vantagens. Para criar um nível novo, basta um novo componente
 * do Spring que implemente esta interface, sobrescrevendo só as vantagens que ele tiver.
 */
public interface NivelClube {

    /** Código usado no campo {@code nivelClube}. */
    String codigo();

    /** Crédito para a próxima compra, sobre o valor dos produtos (sem desconto e sem frete). */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    /** Se o cliente deixa de pagar o frete nesta opção de entrega. */
    default boolean freteGratis(ModalidadeEntrega modalidade) {
        return false;
    }

    /** Se vai um brinde junto com o pedido. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
