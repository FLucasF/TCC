package br.tcc.checkout.dominio.cupom;

import br.tcc.checkout.dto.ItemPedido;
import br.tcc.checkout.util.Arredondador;
import java.math.BigDecimal;
import java.util.List;

public class CupomBemVindo10 implements Cupom {
    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos, BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
        BigDecimal desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
        return Arredondador.arredondarParaCentavos(desconto);
    }
}
