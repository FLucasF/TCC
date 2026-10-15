package com.loja.checkout.servico;

import com.loja.checkout.api.CheckoutRequest;
import com.loja.checkout.api.CheckoutResponse;
import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.clube.ClubeRegistry;
import com.loja.checkout.clube.NivelClubeBeneficio;
import com.loja.checkout.cupom.CupomCalculadora;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.entrega.EntregaCalculadora;
import com.loja.checkout.entrega.EntregaRegistry;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.pagamento.FormaPagamentoCalculadora;
import com.loja.checkout.pagamento.PagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.seguro.SeguroCalculadora;
import com.loja.checkout.util.Arredondamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Orquestra o cálculo do resumo de compra. A escolha de comportamento por caso
 * (modalidade, cupom, nível de clube, forma de pagamento) é sempre delegada a um
 * lookup nos respectivos registries; esta classe só decide a ORDEM das etapas e a
 * ordem de validação dos erros, que é igual para todo pedido.
 */
@Service
public class CheckoutService {

    private final EntregaRegistry entregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final ClubeRegistry clubeRegistry;
    private final PagamentoRegistry pagamentoRegistry;
    private final SeguroCalculadora seguroCalculadora;

    public CheckoutService(EntregaRegistry entregaRegistry,
                            CupomRegistry cupomRegistry,
                            ClubeRegistry clubeRegistry,
                            PagamentoRegistry pagamentoRegistry,
                            SeguroCalculadora seguroCalculadora) {
        this.entregaRegistry = entregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.clubeRegistry = clubeRegistry;
        this.pagamentoRegistry = pagamentoRegistry;
        this.seguroCalculadora = seguroCalculadora;
    }

    public CheckoutResponse calcularResumo(CheckoutRequest request) {
        List<ItemPedido> itens = validarItens(request);
        BigDecimal subtotalProdutos = Arredondamento.monetario(somarSubtotal(itens));

        NivelClubeBeneficio clube = validarNivelClube(request);
        Regiao regiao = validarRegiao(request);
        ModalidadeEntrega modalidade = validarModalidade(request);

        EntregaCalculadora entregaCalculadora = entregaRegistry.buscar(modalidade);
        BigDecimal pesoTotal = somarPeso(itens);
        if (!entregaCalculadora.disponivel(pesoTotal)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        Optional<CupomCalculadora> cupom = validarCupom(request, subtotalProdutos);

        FormaPagamentoCalculadora pagamentoCalculadora = validarFormaPagamento(request);
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        if (!pagamentoCalculadora.parcelasValidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }

        BigDecimal frete = Arredondamento.monetario(entregaCalculadora.calcularCustoBruto(pesoTotal));
        if (clube.isentaFrete()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal seguro = Arredondamento.monetario(seguroCalculadora.calcularBruto(regiao, subtotalProdutos));

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom.isPresent()) {
            descontoCupom = Arredondamento.monetario(cupom.get().calcularDesconto(subtotalProdutos, itens, frete));
        }

        BigDecimal totalPedido = Arredondamento.monetario(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!pagamentoCalculadora.disponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = pagamentoCalculadora.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Arredondamento.monetario(clube.calcularCreditoBruto(subtotalProdutos));
        boolean brinde = clube.temBrinde(subtotalProdutos);

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                entregaCalculadora.getPrazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde);
    }

    private List<ItemPedido> validarItens(CheckoutRequest request) {
        List<ItemRequest> itens = request.getItens();
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens.stream().map(this::validarItem).toList();
    }

    private ItemPedido validarItem(ItemRequest item) {
        if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0
                || item.getQuantidade() == null || item.getQuantidade() <= 0
                || item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemPedido(item.getNome(), item.getPrecoUnitario(), item.getQuantidade(), item.getPesoKg());
    }

    private BigDecimal somarSubtotal(List<ItemPedido> itens) {
        return itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarPeso(List<ItemPedido> itens) {
        return itens.stream().map(ItemPedido::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private NivelClubeBeneficio validarNivelClube(CheckoutRequest request) {
        NivelClube nivel = parseEnum(NivelClube.class, request.getNivelClube());
        if (nivel == null) {
            throw new CheckoutException(CodigoErro.NIVEL_CLUBE_INVALIDO);
        }
        return clubeRegistry.buscar(nivel);
    }

    private Regiao validarRegiao(CheckoutRequest request) {
        Regiao regiao = parseEnum(Regiao.class, request.getRegiao());
        if (regiao == null) {
            throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
        }
        return regiao;
    }

    private ModalidadeEntrega validarModalidade(CheckoutRequest request) {
        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, request.getModalidadeEntrega());
        if (modalidade == null) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INVALIDA);
        }
        return modalidade;
    }

    private Optional<CupomCalculadora> validarCupom(CheckoutRequest request, BigDecimal subtotalProdutos) {
        String codigo = request.getCupom();
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        CupomCalculadora cupomCalculadora = cupomRegistry.buscar(codigo.trim())
                .orElseThrow(() -> new CheckoutException(CodigoErro.CUPOM_INVALIDO));
        if (!cupomCalculadora.aplicavel(subtotalProdutos)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Optional.of(cupomCalculadora);
    }

    private FormaPagamentoCalculadora validarFormaPagamento(CheckoutRequest request) {
        FormaPagamento forma = parseEnum(FormaPagamento.class, request.getFormaPagamento());
        if (forma == null) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return pagamentoRegistry.buscar(forma);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
