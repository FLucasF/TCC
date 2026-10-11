package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Nova forma de pagamento = nova classe @Component implementando esta interface. */
public interface FormaPagamento {

    String codigo();

    boolean parcelasValidas(int parcelas);

    boolean atende(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
