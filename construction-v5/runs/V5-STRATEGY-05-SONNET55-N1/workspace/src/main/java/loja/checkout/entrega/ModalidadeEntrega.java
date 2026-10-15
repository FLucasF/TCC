package loja.checkout.entrega;

import loja.checkout.Codificado;
import loja.checkout.Pedido;

import java.math.BigDecimal;

/** Cada opção de entrega mora na sua própria classe: cobrança, prazo e limitações. */
public interface ModalidadeEntrega extends Codificado {

    boolean atende(Pedido pedido);

    BigDecimal frete(Pedido pedido);

    int prazoDias();
}
