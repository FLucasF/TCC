package br.com.loja.checkout.api;

import br.com.loja.checkout.calculo.CalculadoraResumo;
import br.com.loja.checkout.calculo.PedidoRecusadoException;
import br.com.loja.checkout.calculo.ResumoCompra;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final MontadorPedido montador;
    private final CalculadoraResumo calculadora;

    public ResumoController(MontadorPedido montador, CalculadoraResumo calculadora) {
        this.montador = montador;
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompra resumo(@RequestBody(required = false) RequisicaoResumo requisicao) {
        if (requisicao == null) {
            throw new PedidoRecusadoException("PEDIDO_INVALIDO");
        }
        PedidoValidado validado = montador.montar(requisicao);
        return calculadora.calcular(validado.pedido(), validado.pagamento());
    }

    @ExceptionHandler(PedidoRecusadoException.class)
    public ResponseEntity<ErroResposta> recusado(PedidoRecusadoException excecao) {
        return ResponseEntity.badRequest().body(new ErroResposta(excecao.codigo()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResposta("PEDIDO_INVALIDO"));
    }
}
