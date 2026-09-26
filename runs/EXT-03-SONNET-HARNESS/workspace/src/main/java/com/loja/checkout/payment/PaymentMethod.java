package com.loja.checkout.payment;

import java.math.BigDecimal;

/** Uma forma de pagamento: cada uma sabe suas regras de parcelamento e como ajusta o total. */
public interface PaymentMethod {

    String getCodigo();

    boolean isParcelasValidas(int parcelas);

    boolean isDisponivel(BigDecimal totalProdutosComCupomEFrete);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
