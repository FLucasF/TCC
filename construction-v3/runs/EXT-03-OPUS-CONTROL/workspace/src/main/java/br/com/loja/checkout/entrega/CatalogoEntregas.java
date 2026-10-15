package br.com.loja.checkout.entrega;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Reune todas as opcoes de entrega publicadas no sistema. */
@Component
public class CatalogoEntregas {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public CatalogoEntregas(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream().collect(Collectors.toMap(
                ModalidadeEntrega::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<ModalidadeEntrega> porCodigo(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
