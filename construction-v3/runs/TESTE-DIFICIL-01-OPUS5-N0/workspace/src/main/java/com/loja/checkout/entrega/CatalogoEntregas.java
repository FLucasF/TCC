package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoEntregas extends Catalogo<ModalidadeEntrega> {

    public CatalogoEntregas(List<ModalidadeEntrega> modalidades) {
        super(modalidades);
    }
}
