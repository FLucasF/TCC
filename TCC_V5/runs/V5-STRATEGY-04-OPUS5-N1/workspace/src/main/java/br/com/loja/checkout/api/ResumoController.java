package br.com.loja.checkout.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.checkout.dominio.CalculadoraResumo;
import br.com.loja.checkout.dominio.PedidoRecusadoException;
import br.com.loja.checkout.dominio.Resumo;

@RestController
@RequestMapping("/checkout")
public class ResumoController {

    private final CalculadoraResumo calculadora;

    public ResumoController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public Resumo resumo(@RequestBody ResumoRequest request) {
        return calculadora.calcular(request.paraDominio());
    }

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResponse> recusado(PedidoRecusadoException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.motivo().name()));
    }
}
