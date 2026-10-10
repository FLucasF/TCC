package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.ResultadoClube;
import java.math.BigDecimal;

public interface NivelClube {
    ResultadoClube calcular(BigDecimal subtotalProdutos);
}
