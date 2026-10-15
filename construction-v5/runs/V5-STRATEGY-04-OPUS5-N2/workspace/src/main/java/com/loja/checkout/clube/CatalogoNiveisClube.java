package com.loja.checkout.clube;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoNiveisClube extends Catalogo<NivelClube> {

    public CatalogoNiveisClube(List<NivelClube> niveis) {
        super(niveis);
    }
}
