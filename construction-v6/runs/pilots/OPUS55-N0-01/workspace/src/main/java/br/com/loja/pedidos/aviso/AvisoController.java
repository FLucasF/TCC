package br.com.loja.pedidos.aviso;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.pedidos.comum.ErroNegocio;
import br.com.loja.pedidos.comum.Identificador;

@RestController
public class AvisoController {

    private final AvisoRepositorio avisos;

    public AvisoController(AvisoRepositorio avisos) {
        this.avisos = avisos;
    }

    @PostMapping("/avisos/{id}/leitura")
    public Aviso marcarLido(@PathVariable String id) {
        ErroNegocio naoEncontrado = ErroNegocio.naoEncontrado("AVISO_NAO_ENCONTRADO");
        long numero = Identificador.ler(id).orElseThrow(() -> naoEncontrado);
        avisos.buscar(numero).orElseThrow(() -> naoEncontrado);
        avisos.marcarLido(numero);
        return avisos.buscar(numero).orElseThrow(() -> naoEncontrado);
    }
}
