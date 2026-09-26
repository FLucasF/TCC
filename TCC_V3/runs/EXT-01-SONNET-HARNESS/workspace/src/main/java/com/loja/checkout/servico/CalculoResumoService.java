package com.loja.checkout.servico;

import com.loja.checkout.api.ErroNegocioException;
import com.loja.checkout.api.dto.ItemPedidoDto;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.dominio.ContextoCupom;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CalculoResumoService {

    public ResumoResponse calcular(ResumoRequest request) {
        List<ItemPedido> itens = validarItens(request.itens());
        BigDecimal subtotal = Dinheiro.arredondar(calcularSubtotal(itens));
        BigDecimal pesoTotalKg = calcularPesoTotal(itens);

        NivelClube nivelClube = parseNivelClube(request.nivelClube());
        Regiao regiao = parseRegiao(request.regiao());

        ModalidadeEntrega modalidade = parseModalidade(request.modalidadeEntrega());
        if (!modalidade.disponivel(pesoTotalKg)) {
            throw new ErroNegocioException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = Dinheiro.arredondar(
                nivelClube.freteGratis() ? BigDecimal.ZERO : modalidade.calcularFrete(pesoTotalKg));

        BigDecimal descontoCupom = BigDecimal.ZERO;
        Cupom cupom = parseCupomOpcional(request.cupom());
        if (cupom != null) {
            ContextoCupom contextoCupom = new ContextoCupom(subtotal, frete, itens);
            if (!cupom.aplicavel(contextoCupom)) {
                throw new ErroNegocioException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = Dinheiro.arredondar(cupom.calcularDesconto(contextoCupom));
        }

        BigDecimal baseImposto = subtotal.subtract(descontoCupom);
        BigDecimal imposto = Dinheiro.arredondar(baseImposto.multiply(regiao.percentualImposto()));

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotal.subtract(descontoCupom).add(frete).add(imposto));

        FormaPagamento formaPagamento = parseFormaPagamento(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new ErroNegocioException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new ErroNegocioException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.aplicar(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(resultadoPagamento.totalFinal().subtract(totalPedido));

        BigDecimal creditoProximaCompra = Dinheiro.arredondar(nivelClube.calcularCredito(subtotal));
        boolean brinde = nivelClube.temBrinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                imposto,
                ajustePagamento,
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde
        );
    }

    private List<ItemPedido> validarItens(List<ItemPedidoDto> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroNegocioException("PEDIDO_INVALIDO");
        }
        return itens.stream()
                .map(this::validarItem)
                .toList();
    }

    private ItemPedido validarItem(ItemPedidoDto item) {
        if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ErroNegocioException("PEDIDO_INVALIDO");
        }
        return new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private BigDecimal calcularSubtotal(List<ItemPedido> itens) {
        return itens.stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPesoTotal(List<ItemPedido> itens) {
        return itens.stream()
                .map(item -> item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private NivelClube parseNivelClube(String valor) {
        if (valor == null) {
            throw new ErroNegocioException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return NivelClube.valueOf(valor);
        } catch (IllegalArgumentException excecao) {
            throw new ErroNegocioException("NIVEL_CLUBE_INVALIDO");
        }
    }

    private Regiao parseRegiao(String valor) {
        if (valor == null) {
            throw new ErroNegocioException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(valor);
        } catch (IllegalArgumentException excecao) {
            throw new ErroNegocioException("REGIAO_INVALIDA");
        }
    }

    private ModalidadeEntrega parseModalidade(String valor) {
        if (valor == null) {
            throw new ErroNegocioException("MODALIDADE_INVALIDA");
        }
        try {
            return ModalidadeEntrega.valueOf(valor);
        } catch (IllegalArgumentException excecao) {
            throw new ErroNegocioException("MODALIDADE_INVALIDA");
        }
    }

    private Cupom parseCupomOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Cupom.valueOf(valor);
        } catch (IllegalArgumentException excecao) {
            throw new ErroNegocioException("CUPOM_INVALIDO");
        }
    }

    private FormaPagamento parseFormaPagamento(String valor) {
        if (valor == null) {
            throw new ErroNegocioException("FORMA_PAGAMENTO_INVALIDA");
        }
        try {
            return FormaPagamento.valueOf(valor);
        } catch (IllegalArgumentException excecao) {
            throw new ErroNegocioException("FORMA_PAGAMENTO_INVALIDA");
        }
    }
}
