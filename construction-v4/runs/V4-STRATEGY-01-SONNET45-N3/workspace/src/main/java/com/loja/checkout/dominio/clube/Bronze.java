package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.ResultadoClube;
import java.math.BigDecimal;

public class Bronze implements NivelClube {

    @Override
    public ResultadoClube calcular(BigDecimal subtotalProdutos) {
        return new ResultadoClube(new BigDecimal("0.00"), false, false);
    }
}
