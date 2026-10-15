package com.loja.checkout.entrega;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.CodigoErro;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EntregaService {

    private final Map<String, ModalidadeEntrega> modalidadesPorCodigo;

    public EntregaService(List<ModalidadeEntrega> modalidades) {
        this.modalidadesPorCodigo = modalidades.stream()
                .collect(Collectors.toUnmodifiableMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    public ModalidadeEntrega buscar(String codigo, PedidoContexto contexto) {
        if (codigo == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        ModalidadeEntrega modalidade = modalidadesPorCodigo.get(codigo);
        if (modalidade == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        if (!modalidade.disponivelPara(contexto)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        return modalidade;
    }
}
