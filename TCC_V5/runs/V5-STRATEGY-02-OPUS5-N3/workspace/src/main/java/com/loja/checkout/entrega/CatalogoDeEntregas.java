package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogoDeEntregas extends Catalogo<ModalidadeEntrega> {

    public CatalogoDeEntregas(List<ModalidadeEntrega> modalidades) {
        super(modalidades);
    }
}
