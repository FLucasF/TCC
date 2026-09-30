package com.loja.pedidos.api;

import com.loja.pedidos.dominio.ErroPedido;
import com.loja.pedidos.dominio.ErroPedidoException;
import com.loja.pedidos.servico.PedidoRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Toda resposta de erro sai daqui, sempre no formato {@code {"erro": "CODIGO"}}. */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Pattern CAMINHO_DA_ACAO = Pattern.compile("/pedidos/([^/]+)/acoes/?");

    private final PedidoRepositorio repositorio;

    public TratadorDeErros(PedidoRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @ExceptionHandler(ErroPedidoException.class)
    public ResponseEntity<ErroResposta> erroConhecido(ErroPedidoException excecao) {
        return resposta(excecao.erro());
    }

    /**
     * Corpo que o site mandou torto ou com um valor que nao da para ler. Na
     * criacao isso e um pedido invalido; ao pedir uma acao e uma acao invalida,
     * mas o pedido inexistente vem antes na ordem de verificacao.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoIlegivel(HttpServletRequest requisicao) {
        Matcher acao = CAMINHO_DA_ACAO.matcher(requisicao.getRequestURI());
        if (!acao.matches()) {
            return resposta(ErroPedido.PEDIDO_INVALIDO);
        }
        return repositorio.porId(acao.group(1)).isEmpty()
                ? resposta(ErroPedido.PEDIDO_NAO_ENCONTRADO)
                : resposta(ErroPedido.ACAO_INVALIDA);
    }

    private ResponseEntity<ErroResposta> resposta(ErroPedido erro) {
        return ResponseEntity.status(erro.status()).body(ErroResposta.de(erro));
    }
}
