package com.loja.checkout.api;

import com.loja.checkout.domain.erro.CodigoErro;
import com.loja.checkout.domain.erro.RegraNegocioException;
import com.loja.checkout.servico.CalculadoraResumoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumoService calculadora;

    public CheckoutController(CalculadoraResumoService calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest requisicao) {
        if (requisicao == null) {
            throw new RegraNegocioException(CodigoErro.PEDIDO_INVALIDO);
        }
        return calculadora.calcular(requisicao);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.getCodigo().name()));
    }

    /** Corpo que o site mandou fora do formato combinado: tratamos como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(CodigoErro.PEDIDO_INVALIDO.name()));
    }
}
