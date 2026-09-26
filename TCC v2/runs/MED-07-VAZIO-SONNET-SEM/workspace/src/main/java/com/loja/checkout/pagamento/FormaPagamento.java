package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja. Para aceitar uma nova forma, crie
 * uma implementacao anotada com {@code @Component}: ela e detectada
 * automaticamente pelo {@link FormaPagamentoRegistry}.
 */
public interface FormaPagamento {

    /** Codigo usado no campo "formaPagamento" da requisicao, ex.: "PIX". */
    String codigo();

    /** Se o numero de parcelas informado e permitido para essa forma de pagamento. */
    boolean parcelasValidas(int parcelas);

    /** Se essa forma de pagamento atende o pedido (ex.: limite de valor). */
    boolean disponivel(BigDecimal totalPedido);

    /** Calcula o ajuste, o total final e o valor da parcela. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
