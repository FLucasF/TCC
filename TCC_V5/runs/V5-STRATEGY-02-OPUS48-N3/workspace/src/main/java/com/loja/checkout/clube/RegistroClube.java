package com.loja.checkout.clube;

import com.loja.checkout.comum.RegistroDeCodigos;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegistroClube extends RegistroDeCodigos<NivelClube> {

    public RegistroClube(List<NivelClube> niveis) {
        super(niveis);
    }
}
