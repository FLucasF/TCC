package com.loja.checkout.clube;

import com.loja.checkout.dominio.NivelClube;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Coleta todas as implementações de {@link NivelClubeBeneficio} registradas como
 * componentes Spring e monta o mapa de lookup por nível.
 */
@Component
public class ClubeRegistry {

    private final Map<NivelClube, NivelClubeBeneficio> porNivel;

    public ClubeRegistry(List<NivelClubeBeneficio> beneficios) {
        this.porNivel = beneficios.stream()
                .collect(Collectors.toUnmodifiableMap(NivelClubeBeneficio::getTipo, Function.identity()));
    }

    public NivelClubeBeneficio buscar(NivelClube nivel) {
        return porNivel.get(nivel);
    }
}
