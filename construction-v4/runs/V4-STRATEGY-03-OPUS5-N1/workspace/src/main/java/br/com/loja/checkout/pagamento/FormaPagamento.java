package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.catalogo.Codificado;
import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada forma nova entra como uma implementacao desta
 * interface, com seu ajuste, seu parcelamento e sua limitacao.
 */
public interface FormaPagamento extends Codificado {

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    boolean parcelamentoPermitido(int parcelas);

    /** Se a forma de pagamento atende um pedido deste valor. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
