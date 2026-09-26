package br.tcc.checkout.api;

import br.tcc.checkout.dominio.CheckoutException;
import br.tcc.checkout.dominio.Erros;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ResumoController {

	private final ResumoCompraService servico;

	public ResumoController(ResumoCompraService servico) {
		this.servico = servico;
	}

	@PostMapping("/checkout/resumo")
	public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest pedido) {
		return servico.calcular(pedido);
	}

	@ExceptionHandler(CheckoutException.class)
	public ResponseEntity<Map<String, String>> erro(CheckoutException excecao) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", excecao.codigo()));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> corpoIlegivel(HttpMessageNotReadableException excecao) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", Erros.PEDIDO_INVALIDO));
	}
}
