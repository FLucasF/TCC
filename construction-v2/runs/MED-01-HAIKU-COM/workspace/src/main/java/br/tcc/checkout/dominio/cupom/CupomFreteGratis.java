package br.tcc.checkout.dominio.cupom;

import br.tcc.checkout.dto.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public class CupomFreteGratis implements Cupom {
    private BigDecimal freteCalculado;

    public CupomFreteGratis() {
    }

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos, BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
        return freteCalculado != null ? freteCalculado : BigDecimal.ZERO;
    }

    public void setFreteCalculado(BigDecimal freteCalculado) {
        this.freteCalculado = freteCalculado;
    }
}
