package br.com.loja.checkout.clube;

import br.com.loja.checkout.pedido.Opcao;
import java.math.BigDecimal;

/** As vantagens de um nível do clube. */
public interface NivelClube extends Opcao {

    /** Frete que o cliente deste nível paga, dado o frete da entrega escolhida. */
    BigDecimal frete(BigDecimal freteEntrega);

    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    boolean brinde(BigDecimal subtotalProdutos);
}
