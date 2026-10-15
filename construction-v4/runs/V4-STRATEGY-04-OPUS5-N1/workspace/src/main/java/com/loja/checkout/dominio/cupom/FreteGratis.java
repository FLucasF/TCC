package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Centavos;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** O cliente nao paga o frete: o desconto do cupom fica igual ao valor do frete. */
public final class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return Centavos.arredondar(frete);
    }
}
