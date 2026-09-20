package br.tcc.checkout.dominio.cupom;

import br.tcc.checkout.dto.ItemPedido;
import br.tcc.checkout.util.Arredondador;
import java.math.BigDecimal;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {
    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos, BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            int unidadesGratis = item.getQuantidade() / 3;
            if (unidadesGratis > 0) {
                BigDecimal preco = new BigDecimal(item.getPrecoUnitario().toString());
                desconto = desconto.add(preco.multiply(new BigDecimal(unidadesGratis)));
            }
        }
        return Arredondador.arredondarParaCentavos(desconto);
    }
}
