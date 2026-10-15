package com.loja.checkout.strategy.cupom;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.Item;
import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2Strategy implements CupomStrategy {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, CheckoutRequest request) {
        List<Item> itens = request.getItens();
        BigDecimal desconto = BigDecimal.ZERO;

        for (Item item : itens) {
            int quantidade = item.getQuantidade();
            if (quantidade >= 3) {
                int itemsGratis = quantidade / 3;
                BigDecimal valorItemGratis = MoneyRounder.round(
                    item.getPrecoUnitario() * itemsGratis
                );
                desconto = desconto.add(valorItemGratis);
            }
        }

        return MoneyRounder.round(desconto);
    }

    @Override
    public void validar(BigDecimal subtotalProdutos) {
    }
}
