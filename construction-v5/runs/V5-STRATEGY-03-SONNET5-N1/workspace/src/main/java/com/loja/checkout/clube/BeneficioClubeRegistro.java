package com.loja.checkout.clube;

import com.loja.checkout.comum.RegistroPorChave;
import com.loja.checkout.dominio.NivelClube;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class BeneficioClubeRegistro {

    private final RegistroPorChave<NivelClube, BeneficioClube> registro;

    public BeneficioClubeRegistro(List<BeneficioClube> beneficios) {
        this.registro = new RegistroPorChave<>(beneficios, BeneficioClube::nivel);
    }

    public Optional<BeneficioClube> buscar(NivelClube nivel) {
        return registro.buscar(nivel);
    }
}
