package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Leve3Pague2 implements Cupom {

    private static final int TAMANHO_LOTE = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int unidadesGratis = item.quantidade() / TAMANHO_LOTE;
            if (unidadesGratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
        }
        return Arredondamento.centavos(desconto);
    }
}
