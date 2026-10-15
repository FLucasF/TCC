package loja.cupom;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import loja.dominio.LinhaItem;

/** Um cupom. Para criar outro, basta um novo @Component que implemente esta interface. */
public interface Cupom {

    String codigo();

    /** Valor do desconto, ou vazio se o pedido não cumpre a condição do cupom. */
    Optional<BigDecimal> desconto(ContextoCupom contexto);

    record ContextoCupom(
            List<LinhaItem> itens,
            BigDecimal subtotalProdutos,
            BigDecimal frete,
            boolean entregaTemFrete,
            BooleanSupplier primeiraCompra) {
    }
}
