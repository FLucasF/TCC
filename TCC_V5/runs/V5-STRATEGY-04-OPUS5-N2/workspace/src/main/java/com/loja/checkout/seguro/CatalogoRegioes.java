package com.loja.checkout.seguro;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoRegioes extends Catalogo<Regiao> {

    public CatalogoRegioes() {
        super(List.of(Regiao.values()));
    }
}
