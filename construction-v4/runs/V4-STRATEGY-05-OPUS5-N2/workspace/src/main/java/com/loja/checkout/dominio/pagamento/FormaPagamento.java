package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Erro;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Uma forma de pagamento: tem seu ajuste sobre o total do pedido e suas regras
 * de parcelamento e de aceitacao.
 */
public interface FormaPagamento {

    Catalogo<FormaPagamento> CATALOGO = Catalogo.de(Erro.FORMA_PAGAMENTO_INVALIDA, Map.of(
            "PIX", new Pix(),
            "CARTAO", new Cartao(),
            "BOLETO", new Boleto()));

    Pagamento calcular(BigDecimal totalPedido, int parcelas);

    boolean parcelasPermitidas(int parcelas);

    /** Aceita qualquer pedido, salvo quando a forma de pagamento disser o contrario. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
