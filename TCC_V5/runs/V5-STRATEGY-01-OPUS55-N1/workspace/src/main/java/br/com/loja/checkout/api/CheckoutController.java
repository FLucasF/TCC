package br.com.loja.checkout.api;

import br.com.loja.checkout.resumo.CalculadoraResumo;
import br.com.loja.checkout.resumo.CheckoutException;
import br.com.loja.checkout.resumo.ErroCheckout;
import br.com.loja.checkout.resumo.Resumo;
import br.com.loja.checkout.resumo.SolicitacaoResumo;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumo calculadora;

    public CheckoutController(CalculadoraResumo calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/resumo")
    public Resumo resumo(@RequestBody SolicitacaoResumo solicitacao) {
        return calculadora.calcular(solicitacao);
    }

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public Erro erro(CheckoutException excecao) {
        return new Erro(excecao.erro().name());
    }

    /** Corpo que não dá para ler (JSON quebrado, texto no lugar de número...) é um pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Erro corpoIlegivel(HttpMessageNotReadableException excecao) {
        return new Erro(ErroCheckout.PEDIDO_INVALIDO.name());
    }

    public record Erro(String erro) {
    }
}
