package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavelPara(List<ItemRequest> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete;
    }
}
