package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoEntregas extends Catalogo<PoliticaEntrega> {

    public CatalogoEntregas(List<PoliticaEntrega> politicas) {
        super(politicas);
    }
}
