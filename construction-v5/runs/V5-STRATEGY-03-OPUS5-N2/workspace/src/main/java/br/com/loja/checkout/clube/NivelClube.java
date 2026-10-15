package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Codificavel;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import java.math.BigDecimal;

/** Um nivel do clube da loja: o que o cliente daquele nivel recebe. */
public interface NivelClube extends Codificavel {

    default BigDecimal frete(ModalidadeEntrega modalidade, Pedido pedido) {
        return modalidade.frete(pedido);
    }

    default BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
