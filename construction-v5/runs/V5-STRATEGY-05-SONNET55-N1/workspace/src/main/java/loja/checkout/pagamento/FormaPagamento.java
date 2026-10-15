package loja.checkout.pagamento;

import loja.checkout.Codificado;

import java.math.BigDecimal;

/** Cada forma de pagamento mora na sua própria classe. */
public interface FormaPagamento extends Codificado {

    boolean aceitaParcelas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
