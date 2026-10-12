package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

import br.com.loja.checkout.Codificado;

public interface FormaPagamento extends Codificado {

    boolean aceitaParcelas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    Pagamento pagar(BigDecimal totalPedido, int parcelas);
}
