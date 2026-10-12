package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavelPara(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(SUBTOTAL_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return DESCONTO;
    }
}
