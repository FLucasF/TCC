package com.loja.pedidos.service;

import com.loja.pedidos.client.TransLogClient;
import com.loja.pedidos.dto.*;
import com.loja.pedidos.entity.Produto;
import com.loja.pedidos.exception.ErroNegocio;
import com.loja.pedidos.repository.PedidoRepository;
import com.loja.pedidos.repository.ProdutoRepository;
import com.loja.pedidos.util.ArredondamentoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CalculoService {
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final TransLogClient transLogClient;

    public Resumo calcularResumo(DadosCompra dados) {
        validarDados(dados);

        var subtotal = calcularSubtotal(dados);
        var descontoCupom = calcularDescontoCupom(dados, subtotal);
        var frete = calcularFrete(dados);
        var prazoEntrega = calcularPrazoEntrega(dados);
        var seguro = calcularSeguro(dados, subtotal);
        var totalAntesAjuste = subtotal.subtract(descontoCupom).add(frete).add(seguro);
        var ajuste = calcularAjustePagamento(dados, totalAntesAjuste);
        var totalFinal = totalAntesAjuste.add(ajuste);
        var credito = calcularCreditoClube(dados, subtotal);
        var brinde = verificarBrinde(dados, subtotal);

        var parcelas = dados.getParcelas() != null ? dados.getParcelas() : 1;
        var valorParcela = calcularValorParcela(totalFinal, parcelas, dados.getFormaPagamento());

        return Resumo.builder()
            .subtotalProdutos(ArredondamentoUtil.arredondar(subtotal))
            .descontoCupom(ArredondamentoUtil.arredondar(descontoCupom))
            .frete(ArredondamentoUtil.arredondar(frete))
            .prazoEntregaDias(prazoEntrega)
            .seguro(ArredondamentoUtil.arredondar(seguro))
            .ajustePagamento(ArredondamentoUtil.arredondar(ajuste))
            .totalFinal(ArredondamentoUtil.arredondar(totalFinal))
            .parcelas(parcelas)
            .valorParcela(ArredondamentoUtil.arredondar(valorParcela))
            .creditoProximaCompra(ArredondamentoUtil.arredondar(credito))
            .brinde(brinde)
            .build();
    }

    private void validarDados(DadosCompra dados) {
        if (dados.getItens() == null || dados.getItens().isEmpty()) {
            throw ErroNegocio.pedidoInvalido("Carrinho vazio");
        }

        if (dados.getCliente() == null || dados.getCliente().getCpf() == null || dados.getCliente().getEmail() == null) {
            throw ErroNegocio.pedidoInvalido("CPF ou e-mail ausente");
        }

        Set<String> skusVistos = new HashSet<>();
        for (var item : dados.getItens()) {
            if (item.getSku() == null || item.getSku().trim().isEmpty()) {
                throw ErroNegocio.pedidoInvalido("Item sem SKU");
            }
            if (skusVistos.contains(item.getSku())) {
                throw ErroNegocio.pedidoInvalido("O mesmo SKU duas vezes no carrinho");
            }
            skusVistos.add(item.getSku());

            if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
                throw ErroNegocio.pedidoInvalido("Quantidade zero, negativa ou ausente");
            }
        }

        if (dados.getNivelClube() == null) {
            throw ErroNegocio.nivelClubeInvalido();
        }

        try {
            DadosCompra.NivelClube.valueOf(dados.getNivelClube().name());
        } catch (IllegalArgumentException e) {
            throw ErroNegocio.nivelClubeInvalido();
        }

        if (dados.getRegiao() == null) {
            throw ErroNegocio.regiaoInvalida();
        }

        if (dados.getCep() == null || !dados.getCep().matches("\\d{8}")) {
            throw ErroNegocio.cepInvalido();
        }

        if (dados.getModalidadeEntrega() == null) {
            throw ErroNegocio.modalidadeInvalida();
        }

        if (dados.getFormaPagamento() == null) {
            throw ErroNegocio.formaPagamentoInvalida();
        }

        var parcelas = dados.getParcelas() != null ? dados.getParcelas() : 1;
        validarParcelamento(dados.getFormaPagamento(), parcelas);

        validarProdutos(dados);
        validarCupom(dados);
        validarModalidade(dados);
        validarFormaPagamento(dados);
    }

    private void validarProdutos(DadosCompra dados) {
        for (var item : dados.getItens()) {
            var produto = produtoRepository.findById(item.getSku());
            if (produto.isEmpty()) {
                throw ErroNegocio.produtoInexistente(item.getSku());
            }

            var p = produto.get();
            if (p.isForaDeLinha()) {
                throw ErroNegocio.produtoForaDeLinha(item.getSku());
            }

            if (p.getEstoque() < item.getQuantidade()) {
                throw ErroNegocio.estoqueInsuficiente(item.getSku());
            }
        }
    }

    private void validarCupom(DadosCompra dados) {
        if (dados.getCupom() == null || dados.getCupom().trim().isEmpty()) {
            return;
        }

        var cupom = dados.getCupom().trim().toUpperCase();

        switch (cupom) {
            case "BEMVINDO10":
                validarBemVindo10(dados);
                break;
            case "MENOS50":
                validarMenos50(dados);
                break;
            case "FRETEGRATIS":
                break;
            case "LEVE3PAGUE2":
                break;
            default:
                throw ErroNegocio.cupomInvalido();
        }
    }

    private void validarBemVindo10(DadosCompra dados) {
        var cpf = dados.getCliente().getCpf();
        var pedidosAntigos = pedidoRepository.findByCpfCliente(cpf)
            .stream()
            .filter(p -> {
                var sit = p.getSituacao();
                return sit == com.loja.pedidos.entity.Pedido.SituacaoPedido.PAGO ||
                       sit == com.loja.pedidos.entity.Pedido.SituacaoPedido.ENVIADO ||
                       sit == com.loja.pedidos.entity.Pedido.SituacaoPedido.ENTREGUE;
            })
            .count();

        if (pedidosAntigos > 0) {
            throw ErroNegocio.cupomNaoAplicavel();
        }
    }

    private void validarMenos50(DadosCompra dados) {
        var subtotal = calcularSubtotal(dados);
        if (subtotal.compareTo(new BigDecimal("300.00")) < 0) {
            throw ErroNegocio.cupomNaoAplicavel();
        }
    }

    private void validarModalidade(DadosCompra dados) {
        try {
            DadosCompra.ModalidadeEntrega.valueOf(dados.getModalidadeEntrega().name());
        } catch (IllegalArgumentException e) {
            throw ErroNegocio.modalidadeInvalida();
        }

        var peso = calcularPeso(dados);
        if (dados.getModalidadeEntrega() == DadosCompra.ModalidadeEntrega.MOTOBOY && peso.compareTo(new BigDecimal("5.00")) > 0) {
            throw ErroNegocio.modalidadeIndisponivel();
        }

        if (dados.getModalidadeEntrega() == DadosCompra.ModalidadeEntrega.ECONOMICA ||
            dados.getModalidadeEntrega() == DadosCompra.ModalidadeEntrega.EXPRESSA) {
            var cotacao = transLogClient.cotacao(dados.getCep(), peso, dados.getModalidadeEntrega().name());
            if (cotacao == null) {
                throw ErroNegocio.modalidadeIndisponivel();
            }
        }
    }

    private void validarFormaPagamento(DadosCompra dados) {
        var subtotal = calcularSubtotal(dados);
        var descontoCupom = calcularDescontoCupom(dados, subtotal);
        var frete = calcularFrete(dados);
        var seguro = calcularSeguro(dados, subtotal);
        var totalAntesAjuste = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        if (dados.getFormaPagamento() == DadosCompra.FormaPagamento.BOLETO &&
            totalAntesAjuste.compareTo(new BigDecimal("1000.00")) > 0) {
            throw ErroNegocio.formaPagamentoIndisponivel();
        }
    }

    private void validarParcelamento(DadosCompra.FormaPagamento forma, int parcelas) {
        if (forma == DadosCompra.FormaPagamento.PIX || forma == DadosCompra.FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                throw ErroNegocio.parcelamentoInvalido();
            }
        }

        if (forma == DadosCompra.FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                throw ErroNegocio.parcelamentoInvalido();
            }
        }
    }

    private BigDecimal calcularSubtotal(DadosCompra dados) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : dados.getItens()) {
            var produto = produtoRepository.findById(item.getSku()).orElseThrow();
            var valor = produto.getPreco().multiply(new BigDecimal(item.getQuantidade()));
            subtotal = subtotal.add(valor);
        }
        return ArredondamentoUtil.arredondar(subtotal);
    }

    private BigDecimal calcularDescontoCupom(DadosCompra dados, BigDecimal subtotal) {
        if (dados.getCupom() == null || dados.getCupom().trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        var cupom = dados.getCupom().trim().toUpperCase();

        switch (cupom) {
            case "BEMVINDO10":
                return ArredondamentoUtil.arredondar(subtotal.multiply(new BigDecimal("0.10")));
            case "MENOS50":
                return ArredondamentoUtil.arredondar(new BigDecimal("50.00"));
            case "FRETEGRATIS":
                return ArredondamentoUtil.arredondar(calcularFrete(dados));
            case "LEVE3PAGUE2":
                return calcularDescontoLeve3Pague2(dados, subtotal);
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularDescontoLeve3Pague2(DadosCompra dados, BigDecimal subtotal) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (var item : dados.getItens()) {
            var produto = produtoRepository.findById(item.getSku()).orElseThrow();
            var unidadesGratis = item.getQuantidade() / 3;
            var precoGratis = produto.getPreco().multiply(new BigDecimal(unidadesGratis));
            desconto = desconto.add(precoGratis);
        }
        return ArredondamentoUtil.arredondar(desconto);
    }

    private BigDecimal calcularFrete(DadosCompra dados) {
        switch (dados.getModalidadeEntrega()) {
            case RETIRADA_LOJA:
                return BigDecimal.ZERO;
            case MOTOBOY:
                return ArredondamentoUtil.arredondar(new BigDecimal("18.00"));
            case ECONOMICA:
            case EXPRESSA:
                var peso = calcularPeso(dados);
                var cotacao = transLogClient.cotacao(dados.getCep(), peso, dados.getModalidadeEntrega().name());
                if (cotacao != null) {
                    return ArredondamentoUtil.arredondar(cotacao.getValor());
                }
                return BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }

    private Integer calcularPrazoEntrega(DadosCompra dados) {
        var prazoBase = 0;

        switch (dados.getModalidadeEntrega()) {
            case RETIRADA_LOJA:
                prazoBase = 1;
                break;
            case MOTOBOY:
                prazoBase = 0;
                break;
            case ECONOMICA:
            case EXPRESSA:
                var peso = calcularPeso(dados);
                var cotacao = transLogClient.cotacao(dados.getCep(), peso, dados.getModalidadeEntrega().name());
                if (cotacao != null) {
                    prazoBase = cotacao.getPrazoDias();
                }
                break;
        }

        if ((dados.getModalidadeEntrega() == DadosCompra.ModalidadeEntrega.ECONOMICA ||
             dados.getModalidadeEntrega() == DadosCompra.ModalidadeEntrega.EXPRESSA)) {
            switch (dados.getRegiao()) {
                case NORTE:
                    prazoBase += 3;
                    break;
                case NORDESTE:
                    prazoBase += 2;
                    break;
            }
        }

        return prazoBase;
    }

    private BigDecimal calcularSeguro(DadosCompra dados, BigDecimal subtotal) {
        var percentual = getPercentualSeguro(dados.getRegiao());
        var seguro = subtotal.multiply(percentual).divide(new BigDecimal("100"));
        seguro = ArredondamentoUtil.arredondar(seguro);

        var limite = getLimiteSeguro(dados.getRegiao());
        if (limite != null && seguro.compareTo(limite) > 0) {
            return limite;
        }

        return seguro;
    }

    private BigDecimal getPercentualSeguro(DadosCompra.Regiao regiao) {
        return switch (regiao) {
            case SUDESTE, SUL -> new BigDecimal("1.00");
            case CENTRO_OESTE -> new BigDecimal("1.50");
            case NORTE -> new BigDecimal("2.50");
            case NORDESTE -> new BigDecimal("2.00");
        };
    }

    private BigDecimal getLimiteSeguro(DadosCompra.Regiao regiao) {
        return switch (regiao) {
            case NORTE, NORDESTE -> new BigDecimal("30.00");
            default -> null;
        };
    }

    private BigDecimal calcularAjustePagamento(DadosCompra dados, BigDecimal total) {
        var forma = dados.getFormaPagamento();
        var parcelas = dados.getParcelas() != null ? dados.getParcelas() : 1;

        if (forma == DadosCompra.FormaPagamento.PIX && (dados.getCupom() == null || dados.getCupom().trim().isEmpty())) {
            var desconto = total.multiply(new BigDecimal("0.05"));
            return ArredondamentoUtil.arredondar(desconto.negate());
        }

        if (forma == DadosCompra.FormaPagamento.BOLETO) {
            return ArredondamentoUtil.arredondar(new BigDecimal("3.49"));
        }

        if (forma == DadosCompra.FormaPagamento.CARTAO && parcelas > 3) {
            var taxa = new BigDecimal("0.0199");
            var expoente = parcelas;
            var denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.add(taxa).pow(-expoente));
            var jurosTotal = total.multiply(taxa).divide(denominador, 10, RoundingMode.HALF_EVEN);
            jurosTotal = ArredondamentoUtil.arredondar(jurosTotal);
            return jurosTotal;
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularValorParcela(BigDecimal total, int parcelas, DadosCompra.FormaPagamento forma) {
        if (forma == DadosCompra.FormaPagamento.CARTAO && parcelas > 3) {
            var taxa = new BigDecimal("0.0199");
            var expoente = parcelas;
            var denominador = BigDecimal.ONE.subtract(BigDecimal.ONE.add(taxa).pow(-expoente));
            var parcela = total.multiply(taxa).divide(denominador, 10, RoundingMode.HALF_EVEN);
            return ArredondamentoUtil.arredondar(parcela);
        }

        var parcela = total.divide(new BigDecimal(parcelas), 10, RoundingMode.HALF_EVEN);
        return ArredondamentoUtil.arredondar(parcela);
    }

    private BigDecimal calcularCreditoClube(DadosCompra dados, BigDecimal subtotal) {
        var nivel = dados.getNivelClube();

        switch (nivel) {
            case PRATA:
                var creditoPrata = subtotal.multiply(new BigDecimal("0.02"));
                return ArredondamentoUtil.arredondar(creditoPrata);
            case OURO:
                var creditoOuro = subtotal.multiply(new BigDecimal("0.05"));
                return ArredondamentoUtil.arredondar(creditoOuro);
            default:
                return BigDecimal.ZERO;
        }
    }

    private boolean verificarBrinde(DadosCompra dados, BigDecimal subtotal) {
        if (dados.getNivelClube() != DadosCompra.NivelClube.OURO) {
            return false;
        }

        if (dados.getModalidadeEntrega() == DadosCompra.ModalidadeEntrega.MOTOBOY) {
            return false;
        }

        return subtotal.compareTo(new BigDecimal("500.00")) > 0;
    }

    private BigDecimal calcularPeso(DadosCompra dados) {
        BigDecimal peso = BigDecimal.ZERO;
        for (var item : dados.getItens()) {
            var produto = produtoRepository.findById(item.getSku()).orElseThrow();
            var pesoItem = produto.getPesoKg().multiply(new BigDecimal(item.getQuantidade()));
            peso = peso.add(pesoItem);
        }
        return peso;
    }
}
