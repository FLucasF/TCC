package com.loja.checkout.service;

import com.loja.checkout.dto.ContextoCupom;
import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.enums.Cupom;
import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.enums.ModalidadeEntrega;
import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.enums.Regiao;
import com.loja.checkout.exception.PedidoException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumoService {

    private static final BigDecimal TAXA_JUROS_CARTAO_MES = new BigDecimal("0.0199");
    private static final BigDecimal TARIFA_BOLETO = new BigDecimal("3.49");
    private static final BigDecimal DESCONTO_PIX = new BigDecimal("0.05");
    private static final int PARCELAS_SEM_JUROS_CARTAO = 3;

    public ResumoResponse calcular(ResumoRequest request) {
        List<ItemPedido> itens = validarItens(request.itens());
        NivelClube nivelClube = parseNivelClube(request.nivelClube());
        Regiao regiao = parseRegiao(request.regiao());
        ModalidadeEntrega modalidade = parseModalidade(request.modalidadeEntrega());

        BigDecimal subtotalProdutos = Dinheiro.arredondar(somarSubtotal(itens));
        BigDecimal pesoPedido = somarPeso(itens);

        if (!modalidade.disponivel(pesoPedido)) {
            throw new PedidoException("MODALIDADE_INDISPONIVEL");
        }
        BigDecimal freteCalculado = Dinheiro.arredondar(modalidade.frete(pesoPedido));

        Cupom cupom = parseCupom(request.cupom());
        BigDecimal descontoCupom = Dinheiro.arredondar(BigDecimal.ZERO);
        if (cupom != null) {
            ContextoCupom contextoCupom = new ContextoCupom(itens, subtotalProdutos, freteCalculado);
            cupom.validar(contextoCupom);
            descontoCupom = Dinheiro.arredondar(cupom.desconto(contextoCupom));
        }

        BigDecimal frete = nivelClube.isentaFrete() ? Dinheiro.arredondar(BigDecimal.ZERO) : freteCalculado;
        BigDecimal seguro = Dinheiro.arredondar(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = parseFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelamentoValido(parcelas)) {
            throw new PedidoException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new PedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        BigDecimal valorParcela;
        BigDecimal totalFinal;
        switch (formaPagamento) {
            case PIX -> {
                BigDecimal desconto = Dinheiro.arredondar(totalPedido.multiply(DESCONTO_PIX));
                totalFinal = Dinheiro.arredondar(totalPedido.subtract(desconto));
                valorParcela = totalFinal;
            }
            case BOLETO -> {
                totalFinal = Dinheiro.arredondar(totalPedido.add(TARIFA_BOLETO));
                valorParcela = totalFinal;
            }
            case CARTAO -> {
                if (parcelas <= PARCELAS_SEM_JUROS_CARTAO) {
                    totalFinal = totalPedido;
                    valorParcela = Dinheiro.arredondar(
                            totalFinal.divide(BigDecimal.valueOf(parcelas), MathContext.DECIMAL64));
                } else {
                    valorParcela = Dinheiro.arredondar(calcularParcelaComJuros(totalPedido, parcelas));
                    totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
                }
            }
            default -> throw new PedidoException("FORMA_PAGAMENTO_INVALIDA");
        }
        BigDecimal ajustePagamento = Dinheiro.arredondar(totalFinal.subtract(totalPedido));

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(
                subtotalProdutos.multiply(nivelClube.percentualCredito()));
        boolean brinde = nivelClube.temBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
                creditoProximaCompra,
                brinde
        );
    }

    private BigDecimal calcularParcelaComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS_CARTAO_MES);
        BigDecimal fator = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = fator.subtract(BigDecimal.ONE);
        return totalPedido.multiply(TAXA_JUROS_CARTAO_MES).multiply(fator)
                .divide(denominador, new MathContext(34, RoundingMode.HALF_EVEN));
    }

    private List<ItemPedido> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new PedidoException("PEDIDO_INVALIDO");
        }
        List<ItemPedido> itens = new ArrayList<>();
        for (ItemRequest item : itensRequest) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new PedidoException("PEDIDO_INVALIDO");
            }
            itens.add(new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return itens;
    }

    private BigDecimal somarSubtotal(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal;
    }

    private BigDecimal somarPeso(List<ItemPedido> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private NivelClube parseNivelClube(String valor) {
        if (valor == null) {
            throw new PedidoException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao parseRegiao(String valor) {
        if (valor == null) {
            throw new PedidoException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega parseModalidade(String valor) {
        if (valor == null) {
            throw new PedidoException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoException("MODALIDADE_INVALIDA");
        }
    }

    private Cupom parseCupom(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Cupom.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoException("CUPOM_INVALIDO");
        }
    }

    private FormaPagamento parseFormaPagamento(String valor) {
        if (valor == null) {
            throw new PedidoException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoException("FORMA_PAGAMENTO_INVALIDA");
        }
    }
}
