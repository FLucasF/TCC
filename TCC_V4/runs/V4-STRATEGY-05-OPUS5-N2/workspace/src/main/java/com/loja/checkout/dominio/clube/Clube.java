package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Erro;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Um nivel do clube da loja, com seu conjunto de vantagens. Nivel novo e uma
 * implementacao nova registrada no catalogo.
 */
public interface Clube {

    Catalogo<Clube> CATALOGO = Catalogo.de(Erro.NIVEL_CLUBE_INVALIDO, Map.of(
            "BRONZE", new Bronze(),
            "PRATA", new Prata(),
            "OURO", new Ouro()));

    Vantagens vantagens(BigDecimal subtotalProdutos, BigDecimal freteCalculado);
}
