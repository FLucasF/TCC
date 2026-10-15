package br.tcc.checkout.dominio.cupom;

import br.tcc.checkout.dto.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    String getCodigo();

    boolean ehAplicavel(BigDecimal subtotalProdutos, BigDecimal pesoTotalKg);

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens);
}
