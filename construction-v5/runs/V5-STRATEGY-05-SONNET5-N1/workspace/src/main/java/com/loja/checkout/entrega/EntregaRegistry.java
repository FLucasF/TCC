package com.loja.checkout.entrega;

import com.loja.checkout.dominio.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Coleta todas as implementações de {@link EntregaCalculadora} registradas como
 * componentes Spring e monta o mapa de lookup por modalidade. Novas modalidades de
 * entrega (o enunciado avisa que vão surgir novas toda semana) só precisam de uma nova
 * classe anotada com @Component — nada aqui muda.
 */
@Component
public class EntregaRegistry {

    private final Map<ModalidadeEntrega, EntregaCalculadora> porModalidade;

    public EntregaRegistry(List<EntregaCalculadora> calculadoras) {
        this.porModalidade = calculadoras.stream()
                .collect(Collectors.toUnmodifiableMap(EntregaCalculadora::getTipo, Function.identity()));
    }

    public EntregaCalculadora buscar(ModalidadeEntrega modalidade) {
        return porModalidade.get(modalidade);
    }
}
