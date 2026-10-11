package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.CodigoErro;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Reune todas as opcoes de entrega disponiveis. Como recebe a lista de beans
 * {@link ModalidadeEntrega}, qualquer opcao nova marcada com {@code @Component}
 * aparece aqui sozinha.
 */
@Component
public class RegistroModalidadesEntrega {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public RegistroModalidadesEntrega(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    /** Busca pela opcao; lanca MODALIDADE_INVALIDA se nao existir ou nao vier. */
    public ModalidadeEntrega buscar(String codigo) {
        ModalidadeEntrega modalidade = codigo == null ? null : porCodigo.get(codigo);
        if (modalidade == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return modalidade;
    }
}
