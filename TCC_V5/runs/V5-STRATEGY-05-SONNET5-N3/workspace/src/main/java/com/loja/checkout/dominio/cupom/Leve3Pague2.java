package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {

    private static final int TAMANHO_GRUPO = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(List<Item> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal desconto(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal freteExibido) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : itens) {
            int unidadesGratis = item.quantidade() / TAMANHO_GRUPO;
            if (unidadesGratis > 0) {
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
        }
        return total;
    }
}
