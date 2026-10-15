package com.loja.checkout.entrega;

import com.loja.checkout.comum.RegistroPorChave;
import com.loja.checkout.dominio.ModalidadeEntrega;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OpcoesEntregaRegistro {

    private final RegistroPorChave<ModalidadeEntrega, OpcaoEntrega> registro;

    public OpcoesEntregaRegistro(List<OpcaoEntrega> opcoes) {
        this.registro = new RegistroPorChave<>(opcoes, OpcaoEntrega::modalidade);
    }

    public Optional<OpcaoEntrega> buscar(ModalidadeEntrega modalidade) {
        return registro.buscar(modalidade);
    }
}
