package com.loja.checkout.servico;

import com.loja.checkout.calculo.SeguroCalculador;
import com.loja.checkout.calculo.clube.ClubeCalculador;
import com.loja.checkout.calculo.clube.ClubeBronze;
import com.loja.checkout.calculo.clube.ClubePrata;
import com.loja.checkout.calculo.clube.ClubeOuro;
import com.loja.checkout.calculo.cupom.*;
import com.loja.checkout.calculo.frete.*;
import com.loja.checkout.calculo.pagamento.PagamentoCalculador;
import com.loja.checkout.calculo.pagamento.PagamentoBoleto;
import com.loja.checkout.calculo.pagamento.PagamentoCartao;
import com.loja.checkout.calculo.pagamento.PagamentoPix;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.dto.RequisicaoResumo;
import com.loja.checkout.dto.RespostaResumo;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResumoCheckoutService {
    private final SeguroCalculador seguroCalculador = new SeguroCalculador();

    public RespostaResumo calcular(RequisicaoResumo requisicao) {
        validar(requisicao);

        List<ItemCarrinho> itens = requisicao.itens;
        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(requisicao.modalidadeEntrega);
        NivelClube nivelClube = NivelClube.valueOf(requisicao.nivelClube);
        Regiao regiao = Regiao.valueOf(requisicao.regiao);
        FormaPagamento formaPagamento = FormaPagamento.valueOf(requisicao.formaPagamento);
        Integer parcelas = requisicao.parcelas != null ? requisicao.parcelas : 1;

        BigDecimal subtotalProdutos = calcularSubtotal(itens);
        FreteCalculador freteCalc = obterFreteCalculador(modalidade);
        Double pesoTotal = calcularPesoTotal(itens);

        ClubeCalculador clubeCalc = obterClubeCalculador(nivelClube);
        BigDecimal frete = freteCalc.calcular(pesoTotal);
        if (clubeCalc.temFreteGratis()) {
            frete = Arredondador.arredondar(BigDecimal.ZERO);
        }

        CupomCalculador cupomCalc = obterCupomCalculador(requisicao.cupom, itens);
        BigDecimal descontoCupom = cupomCalc != null ? cupomCalc.calcular(subtotalProdutos, frete) : Arredondador.arredondar(BigDecimal.ZERO);

        BigDecimal totalParcial = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete);
        totalParcial = Arredondador.arredondar(totalParcial);

        BigDecimal seguro = seguroCalculador.calcular(subtotalProdutos, regiao);
        BigDecimal totalPedido = totalParcial.add(seguro);
        totalPedido = Arredondador.arredondar(totalPedido);

        PagamentoCalculador pagtoCalc = obterPagamentoCalculador(formaPagamento, parcelas);
        BigDecimal ajustePagamento = pagtoCalc.calcularAjuste(totalPedido);
        BigDecimal totalFinal = totalPedido.add(ajustePagamento);

        BigDecimal creditoProximaCompra = clubeCalc.calcularCredito(subtotalProdutos);
        boolean temBrinde = clubeCalc.temBrinde(subtotalProdutos);

        RespostaResumo resposta = new RespostaResumo();
        resposta.subtotalProdutos = Arredondador.arredondar(subtotalProdutos);
        resposta.descontoCupom = Arredondador.arredondar(descontoCupom);
        resposta.frete = Arredondador.arredondar(frete);
        resposta.prazoEntregaDias = freteCalc.getPrazo();
        resposta.seguro = Arredondador.arredondar(seguro);
        resposta.ajustePagamento = Arredondador.arredondar(ajustePagamento);
        resposta.totalFinal = Arredondador.arredondar(totalFinal);
        resposta.parcelas = parcelas;
        BigDecimal parcela = pagtoCalc.calcularValorParcela(totalFinal, parcelas);
        resposta.valorParcela = parcela;
        resposta.creditoProximaCompra = creditoProximaCompra;
        resposta.brinde = temBrinde;

        return resposta;
    }

    private void validar(RequisicaoResumo requisicao) throws IllegalArgumentException {
        if (requisicao.itens == null || requisicao.itens.isEmpty()) {
            throw new IllegalArgumentException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : requisicao.itens) {
            if (item.precoUnitario == null || item.precoUnitario <= 0 ||
                item.quantidade == null || item.quantidade <= 0 ||
                item.pesoKg == null || item.pesoKg < 0) {
                throw new IllegalArgumentException("PEDIDO_INVALIDO");
            }
        }

        if (requisicao.nivelClube == null || requisicao.nivelClube.isEmpty()) {
            throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        }

        try {
            NivelClube.valueOf(requisicao.nivelClube);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        }

        if (requisicao.regiao == null || requisicao.regiao.isEmpty()) {
            throw new IllegalArgumentException("REGIAO_INVALIDA");
        }

        try {
            Regiao.valueOf(requisicao.regiao);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("REGIAO_INVALIDA");
        }

        if (requisicao.modalidadeEntrega == null || requisicao.modalidadeEntrega.isEmpty()) {
            throw new IllegalArgumentException("MODALIDADE_INVALIDA");
        }

        try {
            ModalidadeEntrega.valueOf(requisicao.modalidadeEntrega);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("MODALIDADE_INVALIDA");
        }

        ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(requisicao.modalidadeEntrega);
        Double pesoTotal = calcularPesoTotal(requisicao.itens);

        if (modalidade == ModalidadeEntrega.MOTOBOY && pesoTotal > 5.0) {
            throw new IllegalArgumentException("MODALIDADE_INDISPONIVEL");
        }

        if (requisicao.cupom != null && !requisicao.cupom.isEmpty()) {
            validarCupom(requisicao.cupom, requisicao.itens);
        }

        if (requisicao.formaPagamento == null || requisicao.formaPagamento.isEmpty()) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
        }

        try {
            FormaPagamento.valueOf(requisicao.formaPagamento);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
        }

        FormaPagamento formaPagamento = FormaPagamento.valueOf(requisicao.formaPagamento);
        Integer parcelas = requisicao.parcelas != null ? requisicao.parcelas : 1;

        if (formaPagamento == FormaPagamento.PIX || formaPagamento == FormaPagamento.BOLETO) {
            if (parcelas != 1) {
                throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
            }
        } else if (formaPagamento == FormaPagamento.CARTAO) {
            if (parcelas < 1 || parcelas > 12) {
                throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
            }
        }

        if (formaPagamento == FormaPagamento.BOLETO) {
            BigDecimal subtotal = calcularSubtotal(requisicao.itens);
            FreteCalculador freteCalc = obterFreteCalculador(modalidade);
            NivelClube nivel = NivelClube.valueOf(requisicao.nivelClube);
            ClubeCalculador clubeCalc = obterClubeCalculador(nivel);
            BigDecimal frete = clubeCalc.temFreteGratis() ? Arredondador.arredondar(BigDecimal.ZERO) : freteCalc.calcular(pesoTotal);

            CupomCalculador cupomCalc = obterCupomCalculador(requisicao.cupom, requisicao.itens);
            BigDecimal desconto = cupomCalc != null ? cupomCalc.calcular(subtotal, frete) : Arredondador.arredondar(BigDecimal.ZERO);

            Regiao regiao = Regiao.valueOf(requisicao.regiao);
            BigDecimal seguro = seguroCalculador.calcular(subtotal, regiao);

            BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);
            total = Arredondador.arredondar(total);
            total = total.add(Arredondador.arredondar(BigDecimal.valueOf(3.49)));
            total = Arredondador.arredondar(total);

            if (total.compareTo(BigDecimal.valueOf(1000.0)) > 0) {
                throw new IllegalArgumentException("FORMA_PAGAMENTO_INDISPONIVEL");
            }
        }
    }

    private void validarCupom(String codigoCupom, List<ItemCarrinho> itens) {
        try {
            if (!codigoCupom.equals("BEMVINDO10") &&
                !codigoCupom.equals("MENOS50") &&
                !codigoCupom.equals("FRETEGRATIS") &&
                !codigoCupom.equals("LEVE3PAGUE2")) {
                throw new IllegalArgumentException("CUPOM_INVALIDO");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("CUPOM_INVALIDO");
        }

        CupomCalculador cupomCalc = obterCupomCalculador(codigoCupom, itens);
        BigDecimal subtotal = calcularSubtotal(itens);

        if (!cupomCalc.ehAplicavel(subtotal)) {
            throw new IllegalArgumentException("CUPOM_NAO_APLICAVEL");
        }
    }

    private BigDecimal calcularSubtotal(List<ItemCarrinho> itens) {
        BigDecimal subtotal = Arredondador.arredondar(BigDecimal.ZERO);
        for (ItemCarrinho item : itens) {
            BigDecimal precoItem = Arredondador.arredondar(item.precoUnitario);
            BigDecimal valor = precoItem.multiply(BigDecimal.valueOf(item.quantidade));
            valor = Arredondador.arredondar(valor);
            subtotal = subtotal.add(valor);
        }
        return Arredondador.arredondar(subtotal);
    }

    private Double calcularPesoTotal(List<ItemCarrinho> itens) {
        Double peso = 0.0;
        for (ItemCarrinho item : itens) {
            peso += item.pesoKg * item.quantidade;
        }
        return peso;
    }

    private FreteCalculador obterFreteCalculador(ModalidadeEntrega modalidade) {
        return switch (modalidade) {
            case ECONOMICA -> new FreteEconomica();
            case EXPRESSA -> new FreteExpresa();
            case RETIRADA_LOJA -> new FreteRetiradaLoja();
            case MOTOBOY -> new FreteMotoboy();
        };
    }

    private ClubeCalculador obterClubeCalculador(NivelClube nivel) {
        return switch (nivel) {
            case BRONZE -> new ClubeBronze();
            case PRATA -> new ClubePrata();
            case OURO -> new ClubeOuro();
        };
    }

    private CupomCalculador obterCupomCalculador(String codigoCupom, List<ItemCarrinho> itens) {
        if (codigoCupom == null || codigoCupom.isEmpty()) {
            return null;
        }

        return switch (codigoCupom) {
            case "BEMVINDO10" -> new BemVindo10();
            case "MENOS50" -> new Menos50();
            case "FRETEGRATIS" -> new FreteGratis();
            case "LEVE3PAGUE2" -> new Leve3Pague2(itens);
            default -> null;
        };
    }

    private PagamentoCalculador obterPagamentoCalculador(FormaPagamento forma, Integer parcelas) {
        return switch (forma) {
            case PIX -> new PagamentoPix();
            case BOLETO -> new PagamentoBoleto();
            case CARTAO -> new PagamentoCartao(parcelas);
        };
    }
}
