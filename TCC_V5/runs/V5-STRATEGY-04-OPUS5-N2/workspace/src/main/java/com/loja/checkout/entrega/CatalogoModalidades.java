package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoModalidades extends Catalogo<ModalidadeEntrega> {

    public CatalogoModalidades(List<ModalidadeEntrega> modalidades) {
        super(modalidades);
    }
}
