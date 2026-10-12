package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Forma de pagamento. Para criar uma nova, basta registrar outro bean desta interface. */
public interface FormaPagamento {

    String codigo();

    boolean aceitaParcelas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
    }
}
