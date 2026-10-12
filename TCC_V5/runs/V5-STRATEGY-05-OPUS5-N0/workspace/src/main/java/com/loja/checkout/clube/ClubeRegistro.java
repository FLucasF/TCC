package com.loja.checkout.clube;

import com.loja.checkout.domain.Registro;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClubeRegistro extends Registro<NivelClube> {

    public ClubeRegistro(List<NivelClube> niveis) {
        super(niveis, NivelClube::codigo);
    }
}
