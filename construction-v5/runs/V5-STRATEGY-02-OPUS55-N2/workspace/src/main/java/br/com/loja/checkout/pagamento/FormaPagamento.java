package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/** Forma de pagamento. Cada uma decide o parcelamento aceito, se atende o pedido e o ajuste sobre o total. */
public interface FormaPagamento extends Codificado {

    boolean aceitaParcelas(int parcelas);

    boolean atende(BigDecimal totalPedido);

    Pagamento pagar(BigDecimal totalPedido, int parcelas);
}
