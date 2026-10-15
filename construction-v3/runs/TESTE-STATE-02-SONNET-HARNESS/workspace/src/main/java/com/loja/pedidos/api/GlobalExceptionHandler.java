package com.loja.pedidos.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.loja.pedidos.api.dto.ErroResponse;
import com.loja.pedidos.dominio.AcaoInvalidaException;
import com.loja.pedidos.dominio.AcaoNaoPermitidaException;
import com.loja.pedidos.dominio.PedidoInvalidoException;
import com.loja.pedidos.dominio.PedidoNaoEncontradoException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PedidoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratar(PedidoInvalidoException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse("PEDIDO_INVALIDO"));
    }

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratar(PedidoNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse("PEDIDO_NAO_ENCONTRADO"));
    }

    @ExceptionHandler(AcaoInvalidaException.class)
    public ResponseEntity<ErroResponse> tratar(AcaoInvalidaException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse("ACAO_INVALIDA"));
    }

    @ExceptionHandler(AcaoNaoPermitidaException.class)
    public ResponseEntity<ErroResponse> tratar(AcaoNaoPermitidaException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse("ACAO_NAO_PERMITIDA"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratar(HttpMessageNotReadableException e, WebRequest request) {
        String path = request.getDescription(false);
        String codigo = path.endsWith("/acoes") ? "ACAO_INVALIDA" : "PEDIDO_INVALIDO";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ErroResponse(codigo));
    }
}
