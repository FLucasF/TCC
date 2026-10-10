package com.loja.checkout.pagamento;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.dominio.Identificavel;
import java.math.BigDecimal;

/**
 * Uma forma de o cliente pagar. Cada forma diz em quantas vezes aceita,
 * quando atende o pedido, quanto mexe no total e se atrasa a entrega.
 */
public interface FormaPagamento extends Identificavel {

    /** Se esta forma de pagamento aceita esse numero de parcelas. */
    boolean aceitaParcelas(int parcelas);

    /** Se esta forma de pagamento atende um pedido deste valor. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Dias que esta forma de pagamento acrescenta ao prazo de entrega. */
    default int diasAdicionais() {
        return 0;
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, NivelClube nivelClube);
}
