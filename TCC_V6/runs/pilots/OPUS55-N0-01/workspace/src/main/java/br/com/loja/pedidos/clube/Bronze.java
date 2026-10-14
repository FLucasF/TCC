package br.com.loja.pedidos.clube;

import org.springframework.stereotype.Component;

@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }
}
