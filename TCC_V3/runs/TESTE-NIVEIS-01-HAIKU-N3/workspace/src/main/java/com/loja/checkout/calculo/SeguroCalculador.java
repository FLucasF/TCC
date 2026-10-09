package com.loja.checkout.calculo;

import com.loja.checkout.enums.Regiao;
import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class SeguroCalculador {
    public BigDecimal calcular(BigDecimal subtotal, Regiao regiao) {
        BigDecimal percentual = BigDecimal.valueOf(regiao.getPercentualSeguro());
        BigDecimal seguro = subtotal.multiply(percentual);
        return Arredondador.arredondar(seguro);
    }
}
