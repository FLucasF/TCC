package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CupomLeve3Pague2 implements Cupom {

    private List<ItemDto> itens;

    public void setItens(List<ItemDto> itens) { this.itens = itens; }

    @Override public String getCodigo() { return "LEVE3PAGUE2"; }
    @Override public boolean aplicavel(BigDecimal subtotalProdutos) { return true; }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        if (itens == null) return BigDecimal.ZERO.setScale(2);
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            int gratis = item.quantidade() / 3;
            desconto = desconto.add(
                    BigDecimal.valueOf(item.precoUnitario()).multiply(BigDecimal.valueOf(gratis))
            );
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
