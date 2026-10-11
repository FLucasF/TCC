package com.loja.checkout.service.cupom;

import com.loja.checkout.domain.Pedido;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class FreteGratis implements Cupom {
    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete;
    }
}
