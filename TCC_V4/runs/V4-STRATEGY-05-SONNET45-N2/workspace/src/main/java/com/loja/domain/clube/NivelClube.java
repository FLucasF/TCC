package com.loja.domain.clube;

import java.math.BigDecimal;

public interface NivelClube {
    Beneficios calcularBeneficios(BigDecimal subtotalProdutos);
}
