package loja.checkout.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import loja.checkout.dominio.ErroCheckout;
import loja.checkout.servico.CheckoutServico;

@RestController
public class CheckoutController {

    private final CheckoutServico servico;

    public CheckoutController(CheckoutServico servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public RespostaResumo resumo(@RequestBody RequisicaoResumo requisicao) {
        return servico.calcular(requisicao);
    }

    @ExceptionHandler(ErroCheckout.class)
    public ResponseEntity<Map<String, String>> erroCheckout(ErroCheckout e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("erro", e.codigo()));
    }
}
