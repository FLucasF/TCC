package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

/** Só o cadastro: não ganha nada (usa todos os padrões). */
@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
