package com.loja.checkout.service.clube;

import com.loja.checkout.enums.NivelClube;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class BeneficioClubeRegistry {

    private final Map<NivelClube, BeneficioClube> beneficios;

    public BeneficioClubeRegistry(List<BeneficioClube> beneficiosDisponiveis) {
        this.beneficios = new EnumMap<>(NivelClube.class);
        for (BeneficioClube beneficio : beneficiosDisponiveis) {
            beneficios.put(beneficio.getNivel(), beneficio);
        }
    }

    public BeneficioClube obter(NivelClube nivel) {
        return beneficios.get(nivel);
    }
}
