package com.loja.checkout.servico;

import com.loja.checkout.dominio.clube.*;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class NivelClubeFactory {

    private final Map<String, NivelClube> niveis = new HashMap<>();

    public NivelClubeFactory() {
        niveis.put("BRONZE", new Bronze());
        niveis.put("PRATA", new Prata());
        niveis.put("OURO", new Ouro());
    }

    public NivelClube obter(String codigo) {
        return niveis.get(codigo);
    }
}
