package com.loja.checkout.servico;

import com.loja.checkout.dominio.*;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dto.*;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CalculadoraResumo {

    private final CupomFactory cupomFactory;
    private final ModalidadeEntregaFactory modalidadeFactory;
    private final FormaPagamentoFactory pagamentoFactory;
    private final NivelClubeFactory clubeFactory;

    public CalculadoraResumo(
        CupomFactory cupomFactory,
        ModalidadeEntregaFactory modalidadeFactory,
        FormaPagamentoFactory pagamentoFactory,
        NivelClubeFactory clubeFactory
    ) {
        this.cupomFactory = cupomFactory;
        this.modalidadeFactory = modalidadeFactory;
        this.pagamentoFactory = pagamentoFactory;
        this.clubeFactory = clubeFactory;
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        validarPedidoBasico(pedido);

        NivelClube nivelClube = obterNivelClube(pedido.nivelClube());
        Regiao regiao = obterRegiao(pedido.regiao());
        ModalidadeEntrega modalidade = obterModalidadeEntrega(pedido.modalidadeEntrega());

        ContextoPedido contexto = criarContexto(pedido);

        validarDisponibilidadeModalidade(modalidade, contexto);

        BigDecimal freteBruto = calcularFreteBruto(modalidade, contexto);
        int prazoEntrega = modalidade.calcular(contexto).prazoEntregaDias();

        Cupom cupom = obterCupom(pedido.cupom());
        validarCupom(cupom, contexto, freteBruto);

        BigDecimal descontoCupom = calcularDescontoCupom(cupom, contexto, freteBruto);

        ResultadoClube resultadoClube = nivelClube.calcular(contexto.getSubtotalProdutos());
        BigDecimal frete = aplicarFreteGratisSeNecessario(freteBruto, resultadoClube);

        BigDecimal seguro = calcularSeguro(contexto.getSubtotalProdutos(), regiao);
        BigDecimal totalPedido = calcularTotalPedido(
            contexto.getSubtotalProdutos(),
            descontoCupom,
            frete,
            seguro
        );

        FormaPagamento formaPagamento = obterFormaPagamento(pedido.formaPagamento());
        int parcelas = obterParcelas(pedido);

        validarParcelamento(formaPagamento, parcelas);
        validarDisponibilidadeFormaPagamento(formaPagamento, totalPedido);

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
            contexto.getSubtotalProdutos(),
            descontoCupom,
            frete,
            prazoEntrega,
            seguro,
            resultadoPagamento.ajustePagamento(),
            resultadoPagamento.totalFinal(),
            resultadoPagamento.parcelas(),
            resultadoPagamento.valorParcela(),
            resultadoClube.creditoProximaCompra(),
            resultadoClube.brinde()
        );
    }

    private void validarPedidoBasico(PedidoRequest pedido) {
        if (pedido.itens() == null || pedido.itens().isEmpty()) {
            throw new ErroNegocioException("PEDIDO_INVALIDO");
        }

        for (ItemCarrinho item : pedido.itens()) {
            if (item.precoUnitario() == null ||
                item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0 ||
                item.quantidade() == null ||
                item.quantidade() <= 0 ||
                item.pesoKg() == null ||
                item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ErroNegocioException("PEDIDO_INVALIDO");
            }
        }
    }

    private NivelClube obterNivelClube(String codigo) {
        if (codigo == null) {
            throw new ErroNegocioException("NIVEL_CLUBE_INVALIDO");
        }
        NivelClube nivel = clubeFactory.obter(codigo);
        if (nivel == null) {
            throw new ErroNegocioException("NIVEL_CLUBE_INVALIDO");
        }
        return nivel;
    }

    private Regiao obterRegiao(String codigo) {
        if (codigo == null) {
            throw new ErroNegocioException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new ErroNegocioException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega obterModalidadeEntrega(String codigo) {
        if (codigo == null) {
            throw new ErroNegocioException("MODALIDADE_INVALIDA");
        }
        ModalidadeEntrega modalidade = modalidadeFactory.obter(codigo);
        if (modalidade == null) {
            throw new ErroNegocioException("MODALIDADE_INVALIDA");
        }
        return modalidade;
    }

    private ContextoPedido criarContexto(PedidoRequest pedido) {
        ContextoPedido contexto = new ContextoPedido(pedido.itens());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal pesoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : pedido.itens()) {
            BigDecimal valorItem = item.precoUnitario()
                .multiply(BigDecimal.valueOf(item.quantidade()));
            subtotal = subtotal.add(valorItem);

            BigDecimal pesoItem = item.pesoKg()
                .multiply(BigDecimal.valueOf(item.quantidade()));
            pesoTotal = pesoTotal.add(pesoItem);
        }

        contexto.setSubtotalProdutos(Arredondamento.arredondar(subtotal));
        contexto.setPesoTotal(pesoTotal);

        return contexto;
    }

    private void validarDisponibilidadeModalidade(ModalidadeEntrega modalidade, ContextoPedido contexto) {
        if (!modalidade.estaDisponivel(contexto)) {
            throw new ErroNegocioException("MODALIDADE_INDISPONIVEL");
        }
    }

    private BigDecimal calcularFreteBruto(ModalidadeEntrega modalidade, ContextoPedido contexto) {
        return modalidade.calcular(contexto).frete();
    }

    private Cupom obterCupom(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return null;
        }
        Cupom cupom = cupomFactory.obter(codigo);
        if (cupom == null) {
            throw new ErroNegocioException("CUPOM_INVALIDO");
        }
        return cupom;
    }

    private void validarCupom(Cupom cupom, ContextoPedido contexto, BigDecimal frete) {
        if (cupom != null && !cupom.podeAplicar(contexto, frete)) {
            throw new ErroNegocioException("CUPOM_NAO_APLICAVEL");
        }
    }

    private BigDecimal calcularDescontoCupom(Cupom cupom, ContextoPedido contexto, BigDecimal frete) {
        if (cupom == null) {
            return new BigDecimal("0.00");
        }
        return cupom.calcularDesconto(contexto, frete);
    }

    private BigDecimal aplicarFreteGratisSeNecessario(BigDecimal frete, ResultadoClube resultadoClube) {
        if (resultadoClube.freteGratis()) {
            return new BigDecimal("0.00");
        }
        return frete;
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        BigDecimal seguro = subtotalProdutos.multiply(regiao.getPercentualSeguro());
        return Arredondamento.arredondar(seguro);
    }

    private BigDecimal calcularTotalPedido(
        BigDecimal subtotal,
        BigDecimal desconto,
        BigDecimal frete,
        BigDecimal seguro
    ) {
        return subtotal.subtract(desconto).add(frete).add(seguro);
    }

    private FormaPagamento obterFormaPagamento(String codigo) {
        if (codigo == null) {
            throw new ErroNegocioException("FORMA_PAGAMENTO_INVALIDA");
        }
        FormaPagamento forma = pagamentoFactory.obter(codigo);
        if (forma == null) {
            throw new ErroNegocioException("FORMA_PAGAMENTO_INVALIDA");
        }
        return forma;
    }

    private int obterParcelas(PedidoRequest pedido) {
        return pedido.parcelas() != null ? pedido.parcelas() : 1;
    }

    private void validarParcelamento(FormaPagamento formaPagamento, int parcelas) {
        formaPagamento.validarParcelas(parcelas);
    }

    private void validarDisponibilidadeFormaPagamento(FormaPagamento formaPagamento, BigDecimal totalPedido) {
        if (!formaPagamento.estaDisponivel(totalPedido)) {
            throw new ErroNegocioException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }
}
