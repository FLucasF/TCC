package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para criar uma nova basta implementar esta interface
 * e anotar a classe com {@code @Component}.
 */
public interface FormaPagamento {

    /** Codigo enviado pelo site, ex.: PIX. */
    String codigo();

    /** Se a quantidade de parcelas e permitida nesta forma de pagamento. */
    boolean parcelasPermitidas(int parcelas);

    /** Se a forma de pagamento atende o pedido (ex.: boleto tem teto de valor). */
    default boolean atende(ContextoPagamento contexto) {
        return true;
    }

    /** Total final e valor da parcela. */
    ResultadoPagamento calcular(ContextoPagamento contexto, int parcelas);

    /** Divisao simples do total em parcelas, arredondada para centavos. */
    default BigDecimal parcelaSimples(BigDecimal total, int parcelas) {
        return com.loja.checkout.comum.Dinheiro.centavos(
                com.loja.checkout.comum.Dinheiro.dividir(total, BigDecimal.valueOf(parcelas)));
    }
}
