package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

/**
 * Só o cadastro, não ganha nada. Usa todos os padrões da interface.
 */
@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
