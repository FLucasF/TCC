package loja.pagamento;

import java.math.BigDecimal;

import loja.dominio.NegocioException;

/** Uma forma de pagamento. Para criar outra, basta um novo @Component que implemente esta interface. */
public interface FormaPagamento {

    String codigo();

    /** Por padrão, só à vista. */
    default void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw NegocioException.invalido("PARCELAMENTO_INVALIDO");
        }
    }

    /** Lança FORMA_PAGAMENTO_INDISPONIVEL se esta forma não atende o pedido. */
    default void validarDisponibilidade(BigDecimal totalPedido) {
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas, boolean comCupom);

    record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
    }
}
