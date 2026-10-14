package br.com.loja.pedidos.aviso;

import br.com.loja.pedidos.comum.ErroNegocio;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvisoController {

    private final AvisoRepositorio avisos;

    public AvisoController(AvisoRepositorio avisos) {
        this.avisos = avisos;
    }

    @PostMapping("/avisos/{id}/leitura")
    public AvisoResposta marcarLido(@PathVariable String id) {
        long numero;
        try {
            numero = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw naoEncontrado();
        }
        return avisos.marcarLido(numero).map(AvisoResposta::de).orElseThrow(AvisoController::naoEncontrado);
    }

    private static ErroNegocio naoEncontrado() {
        return ErroNegocio.naoEncontrado("AVISO_NAO_ENCONTRADO");
    }
}
