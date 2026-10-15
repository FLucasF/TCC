package br.com.loja.pedidos.comum;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(ErroNegocio.class)
    public ResponseEntity<Map<String, String>> negocio(ErroNegocio erro) {
        return resposta(erro.status(), erro.codigo());
    }

    /** JSON malformado: nos dados da compra é um pedido inválido; nas demais operações, requisição inválida. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> jsonInvalido(HttpServletRequest requisicao) {
        String caminho = requisicao.getRequestURI();
        boolean dadosDaCompra = caminho.endsWith("/checkout/resumo") || caminho.endsWith("/pedidos");
        return resposta(422, dadosDaCompra ? "PEDIDO_INVALIDO" : "REQUISICAO_INVALIDA");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> rotaInexistente() {
        return resposta(404, "NAO_ENCONTRADO");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> inesperado(Exception erro) {
        if (erro instanceof ErrorResponse protocolo) {
            // erros de protocolo HTTP (método não permitido, tipo de conteúdo não suportado etc.)
            return resposta(protocolo.getStatusCode().value(), "REQUISICAO_INVALIDA");
        }
        log.error("Erro inesperado", erro);
        return resposta(500, "ERRO_INTERNO");
    }

    private static ResponseEntity<Map<String, String>> resposta(int status, String codigo) {
        return ResponseEntity.status(status).body(Map.of("erro", codigo));
    }
}
