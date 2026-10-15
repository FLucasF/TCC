package com.loja.resumo.pagamento;

import com.loja.resumo.Codigado;
import java.math.BigDecimal;

public interface FormaPagamento extends Codigado {

    int maxParcelas();

    Pagamento aplicar(BigDecimal total, int parcelas);

    default void verificarDisponivel(BigDecimal total) {
    }
}
