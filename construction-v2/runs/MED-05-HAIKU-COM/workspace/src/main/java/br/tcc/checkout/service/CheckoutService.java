package br.tcc.checkout.service;

import br.tcc.checkout.dto.CheckoutRequisicaoDto;
import br.tcc.checkout.dto.CheckoutRespostaDto;
import br.tcc.checkout.dto.ItemDto;
import br.tcc.checkout.model.Cupom;
import br.tcc.checkout.model.FormaPagamento;
import br.tcc.checkout.model.Item;
import br.tcc.checkout.model.ModalidadeEntrega;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    public CheckoutRespostaDto calcularResumo(CheckoutRequisicaoDto requisicao) {
        List<Item> itens = converterItens(requisicao.itens());
        ModalidadeEntrega modalidade = obterEValidarModalidade(requisicao);
        Cupom cupomEnum = obterEValidarCupom(requisicao, itens, modalidade);
        FormaPagamento formaPagamento = obterEValidarFormaPagamento(requisicao);
        Integer parcelas = requisicao.parcelas() != null ? requisicao.parcelas() : 1;

        validarParcelamento(formaPagamento, parcelas);

        Double pesoTotal = calcularPesoTotal(itens);
        validarPesoParaModalidade(modalidade, pesoTotal);

        Double subtotalProdutos = calcularSubtotalProdutos(itens);
        Double frete = arredondarMeioParaPar(modalidade.calcularFrete(pesoTotal));

        Double descontoCupom = 0.0;
        if (cupomEnum != null) {
            descontoCupom = arredondarMeioParaPar(cupomEnum.calcularDesconto(subtotalProdutos, frete, itens));
        }

        Double totalPedido = arredondarMeioParaPar(subtotalProdutos - descontoCupom + frete);
        validarDisponibilidadeFormaPagamento(formaPagamento, totalPedido);

        Double ajustePagamento = arredondarMeioParaPar(formaPagamento.calcularAjuste(totalPedido, parcelas));
        Double totalFinal = arredondarMeioParaPar(totalPedido + ajustePagamento);

        Double valorParcela = calcularValorParcela(totalFinal, parcelas);

        return new CheckoutRespostaDto(
            subtotalProdutos,
            descontoCupom,
            frete,
            modalidade.getPrazoEmDias(),
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela
        );
    }

    private ModalidadeEntrega obterEValidarModalidade(CheckoutRequisicaoDto requisicao) {
        if (requisicao.modalidadeEntrega() == null || requisicao.modalidadeEntrega().isBlank()) {
            throw new IllegalArgumentException("MODALIDADE_INVALIDA");
        }

        try {
            return ModalidadeEntrega.valueOf(requisicao.modalidadeEntrega());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("MODALIDADE_INVALIDA");
        }
    }

    private Cupom obterEValidarCupom(CheckoutRequisicaoDto requisicao, List<Item> itens, ModalidadeEntrega modalidade) {
        if (requisicao.cupom() == null || requisicao.cupom().isBlank()) {
            return null;
        }

        Cupom cupom;
        try {
            cupom = Cupom.valueOf(requisicao.cupom());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("CUPOM_INVALIDO");
        }

        Double subtotalProdutos = calcularSubtotalProdutos(itens);
        Double pesoTotal = calcularPesoTotal(itens);
        Double frete = modalidade.calcularFrete(pesoTotal);

        if (!cupom.ehAplicavel(subtotalProdutos, frete, itens)) {
            throw new IllegalArgumentException("CUPOM_NAO_APLICAVEL");
        }

        return cupom;
    }

    private FormaPagamento obterEValidarFormaPagamento(CheckoutRequisicaoDto requisicao) {
        if (requisicao.formaPagamento() == null || requisicao.formaPagamento().isBlank()) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
        }

        try {
            return FormaPagamento.valueOf(requisicao.formaPagamento());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
        }
    }

    private void validarPedido(CheckoutRequisicaoDto requisicao) {
        if (requisicao.itens() == null || requisicao.itens().isEmpty()) {
            throw new IllegalArgumentException("PEDIDO_INVALIDO");
        }

        for (ItemDto item : requisicao.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario() <= 0 ||
                item.quantidade() == null || item.quantidade() <= 0 ||
                item.pesoKg() == null || item.pesoKg() < 0) {
                throw new IllegalArgumentException("PEDIDO_INVALIDO");
            }
        }
    }

    private void validarPesoParaModalidade(ModalidadeEntrega modalidade, Double pesoTotal) {
        if (modalidade.temLimitacao() && pesoTotal > modalidade.getLimitePeso()) {
            throw new IllegalArgumentException("MODALIDADE_INDISPONIVEL");
        }
    }

    private void validarParcelamento(FormaPagamento formaPagamento, Integer parcelas) {
        if (!formaPagamento.ehParcelamentoValido(parcelas)) {
            throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
        }
    }

    private void validarDisponibilidadeFormaPagamento(FormaPagamento formaPagamento, Double totalPedido) {
        if (!formaPagamento.ehDisponivel(totalPedido)) {
            throw new IllegalArgumentException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    private List<Item> converterItens(List<ItemDto> itensDto) {
        validarPedido(new CheckoutRequisicaoDto(itensDto, null, null, null, null));
        return itensDto.stream()
            .map(dto -> new Item(dto.nome(), dto.precoUnitario(), dto.quantidade(), dto.pesoKg()))
            .toList();
    }

    private Double calcularSubtotalProdutos(List<Item> itens) {
        return arredondarMeioParaPar(itens.stream()
            .mapToDouble(item -> item.getPrecoUnitario() * item.getQuantidade())
            .sum());
    }

    private Double calcularPesoTotal(List<Item> itens) {
        return itens.stream()
            .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
            .sum();
    }

    private Double calcularValorParcela(Double totalFinal, Integer parcelas) {
        return arredondarMeioParaPar(totalFinal / parcelas);
    }

    private Double arredondarMeioParaPar(Double valor) {
        BigDecimal bd = new BigDecimal(valor.toString());
        bd = bd.setScale(2, RoundingMode.HALF_EVEN);
        return bd.doubleValue();
    }
}
