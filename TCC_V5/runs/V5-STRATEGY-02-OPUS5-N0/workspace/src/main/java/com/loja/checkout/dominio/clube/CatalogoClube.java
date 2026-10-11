package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoClube extends Catalogo<PoliticaClube> {

    public CatalogoClube(List<PoliticaClube> politicas) {
        super(politicas);
    }
}
