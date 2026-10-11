package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

/** Uma forma de pagar: quantas parcelas aceita, o que aceita cobrar e como ajusta o total. */
public interface FormaPagamento extends Codificavel {

    ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas);

    boolean parcelamentoPermitido(int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
