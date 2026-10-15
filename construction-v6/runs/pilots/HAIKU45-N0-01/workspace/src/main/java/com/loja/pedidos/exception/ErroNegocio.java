package com.loja.pedidos.exception;

import org.springframework.http.HttpStatus;

public class ErroNegocio extends RuntimeException {
    private final String codigo;
    private final HttpStatus status;

    public ErroNegocio(String codigo, String mensagem, HttpStatus status) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ErroNegocio pedidoInvalido(String mensagem) {
        return new ErroNegocio("PEDIDO_INVALIDO", mensagem, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio produtoInexistente(String sku) {
        return new ErroNegocio("PRODUTO_INEXISTENTE", "Produto não existe: " + sku, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio produtoForaDeLinha(String sku) {
        return new ErroNegocio("PRODUTO_FORA_DE_LINHA", "Produto fora de linha: " + sku, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio estoqueInsuficiente(String sku) {
        return new ErroNegocio("ESTOQUE_INSUFICIENTE", "Estoque insuficiente para: " + sku, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio nivelClubeInvalido() {
        return new ErroNegocio("NIVEL_CLUBE_INVALIDO", "Nível do clube inválido", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio regiaoInvalida() {
        return new ErroNegocio("REGIAO_INVALIDA", "Região inválida", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio cepInvalido() {
        return new ErroNegocio("CEP_INVALIDO", "CEP inválido", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio modalidadeInvalida() {
        return new ErroNegocio("MODALIDADE_INVALIDA", "Modalidade de entrega inválida", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio modalidadeIndisponivel() {
        return new ErroNegocio("MODALIDADE_INDISPONIVEL", "Modalidade de entrega indisponível para este pedido", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio transportadoraIndisponivel() {
        return new ErroNegocio("TRANSPORTADORA_INDISPONIVEL", "Transportadora indisponível", HttpStatus.SERVICE_UNAVAILABLE);
    }

    public static ErroNegocio cupomInvalido() {
        return new ErroNegocio("CUPOM_INVALIDO", "Cupom inválido", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio cupomNaoAplicavel() {
        return new ErroNegocio("CUPOM_NAO_APLICAVEL", "Cupom não é aplicável para este pedido", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio formaPagamentoInvalida() {
        return new ErroNegocio("FORMA_PAGAMENTO_INVALIDA", "Forma de pagamento inválida", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio parcelamentoInvalido() {
        return new ErroNegocio("PARCELAMENTO_INVALIDO", "Número de parcelas inválido", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio formaPagamentoIndisponivel() {
        return new ErroNegocio("FORMA_PAGAMENTO_INDISPONIVEL", "Forma de pagamento indisponível para este pedido", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ErroNegocio pedidoNaoEncontrado() {
        return new ErroNegocio("PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado", HttpStatus.NOT_FOUND);
    }

    public static ErroNegocio operacaoNaoPermitida() {
        return new ErroNegocio("OPERACAO_NAO_PERMITIDA", "Operação não permitida no estado atual do pedido", HttpStatus.CONFLICT);
    }

    public static ErroNegocio pedidoExpirado() {
        return new ErroNegocio("PEDIDO_EXPIRADO", "Pagamento chegou após 48 horas do registro", HttpStatus.CONFLICT);
    }

    public static ErroNegocio produtoNaoEncontrado() {
        return new ErroNegocio("PRODUTO_NAO_ENCONTRADO", "Produto não encontrado", HttpStatus.NOT_FOUND);
    }

    public static ErroNegocio clienteNaoEncontrado() {
        return new ErroNegocio("CLIENTE_NAO_ENCONTRADO", "Cliente não encontrado", HttpStatus.NOT_FOUND);
    }

    public static ErroNegocio avisoNaoEncontrado() {
        return new ErroNegocio("AVISO_NAO_ENCONTRADO", "Aviso não encontrado", HttpStatus.NOT_FOUND);
    }
}
