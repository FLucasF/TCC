package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.service.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CupomLeve3Pague2 implements ProcessadorCupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int gratuitos = item.quantidade() / 3;
            if (gratuitos > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(new BigDecimal(gratuitos)));
            }
        }
        return Moeda.arredondar(desconto);
    }
}
