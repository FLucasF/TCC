package com.loja.checkout.cupom;

import com.loja.checkout.dominio.PedidoContext;
import java.math.BigDecimal;

/**
 * Um cupom promocional. Para lancar uma promocao nova, crie uma implementacao
 * anotada com {@code @Component}: ela e detectada automaticamente pelo
 * {@link CupomRegistry}.
 */
public interface Cupom {

    /** Codigo do cupom, sempre em maiusculas, ex.: "BEMVINDO10". */
    String codigo();

    /** Se as condicoes do cupom sao atendidas pelo pedido (ex.: valor minimo). */
    boolean aplicavel(PedidoContext pedido);

    /** Calcula o desconto em reais. {@code frete} e o valor de frete ja calculado para o pedido. */
    BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal frete);
}
