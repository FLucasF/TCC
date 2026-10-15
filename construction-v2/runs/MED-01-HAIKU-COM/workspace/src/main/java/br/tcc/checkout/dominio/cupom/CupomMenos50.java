package br.tcc.checkout.dominio.cupom;

import br.tcc.checkout.dto.ItemPedido;
import br.tcc.checkout.util.Arredondador;
import java.math.BigDecimal;
import java.util.List;

public class CupomMenos50 implements Cupom {
    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos, BigDecimal pesoTotalKg) {
        return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
        return Arredondador.arredondarParaCentavos(new BigDecimal("50.00"));
    }
}
