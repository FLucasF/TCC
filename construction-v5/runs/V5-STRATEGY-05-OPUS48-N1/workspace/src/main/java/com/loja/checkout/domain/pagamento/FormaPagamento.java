package com.loja.checkout.domain.pagamento;

import com.loja.checkout.domain.comum.ComCodigo;

import java.math.BigDecimal;

/**
 * Uma forma de pagar. Cada forma tem seu proprio ajuste sobre o total (Pix
 * desconta, boleto soma tarifa, cartao parcela com ou sem juros), suas regras
 * de parcelamento e suas limitacoes, entao cada uma mora na sua classe.
 */
public interface FormaPagamento extends ComCodigo {

    boolean parcelasValidas(int parcelas);

    /** Se a forma de pagamento atende um pedido com este total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
