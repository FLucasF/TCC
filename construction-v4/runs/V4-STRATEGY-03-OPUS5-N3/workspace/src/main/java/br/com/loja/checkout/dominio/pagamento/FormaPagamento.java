package br.com.loja.checkout.dominio.pagamento;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Identificavel;

/**
 * Uma forma de pagamento. Cada forma guarda seu ajuste sobre o total, quantas
 * parcelas aceita e quando nao atende o pedido.
 */
public interface FormaPagamento extends Identificavel {

    boolean parcelasPermitidas(int parcelas);

    /** Se esta forma atende um pedido deste total. */
    boolean atende(Dinheiro totalPedido);

    Cobranca cobrar(Dinheiro totalPedido, int parcelas);
}
