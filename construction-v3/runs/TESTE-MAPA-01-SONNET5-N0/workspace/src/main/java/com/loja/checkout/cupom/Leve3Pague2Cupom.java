package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2Cupom implements CupomStrategy {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal freteExibido) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            if (unidadesGratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
        }
        return desconto;
    }
}
