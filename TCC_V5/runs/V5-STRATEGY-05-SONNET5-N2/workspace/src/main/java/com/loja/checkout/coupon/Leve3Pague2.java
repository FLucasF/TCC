package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {

    private static final int TAMANHO_DO_LOTE = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavelPara(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int unidadesGratis = item.quantidade() / TAMANHO_DO_LOTE;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return desconto;
    }
}
