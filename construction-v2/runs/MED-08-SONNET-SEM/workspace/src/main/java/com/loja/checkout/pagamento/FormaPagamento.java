package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Ponto de extensao: cada nova forma de pagamento vira uma implementacao
 * desta interface, sem alterar o restante do sistema.
 */
public interface FormaPagamento {

    String codigo();

    boolean parcelasValidas(int parcelas);

    boolean disponivelPara(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
