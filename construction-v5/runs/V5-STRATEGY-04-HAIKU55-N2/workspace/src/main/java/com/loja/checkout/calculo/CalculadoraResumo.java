package com.loja.checkout.calculo;

import static com.loja.checkout.calculo.CodigoErro.CUPOM_INVALIDO;
import static com.loja.checkout.calculo.CodigoErro.CUPOM_NAO_APLICAVEL;
import static com.loja.checkout.calculo.CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL;
import static com.loja.checkout.calculo.CodigoErro.FORMA_PAGAMENTO_INVALIDA;
import static com.loja.checkout.calculo.CodigoErro.MODALIDADE_INDISPONIVEL;
import static com.loja.checkout.calculo.CodigoErro.MODALIDADE_INVALIDA;
import static com.loja.checkout.calculo.CodigoErro.NIVEL_CLUBE_INVALIDO;
import static com.loja.checkout.calculo.CodigoErro.PARCELAMENTO_INVALIDO;
import static com.loja.checkout.calculo.CodigoErro.PEDIDO_INVALIDO;
import static com.loja.checkout.calculo.CodigoErro.REGIAO_INVALIDA;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.Pagamento;
import com.loja.checkout.web.ItemRequisicao;
import com.loja.checkout.web.RequisicaoResumo;
import com.loja.checkout.web.ResumoCompra;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(Catalogo<ModalidadeEntrega> modalidades,
                             Catalogo<Cupom> cupons,
                             Catalogo<NivelClube> niveis,
                             Catalogo<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.niveis = niveis;
        this.formasPagamento = formasPagamento;
    }

    public ResumoCompra calcular(RequisicaoResumo requisicao) {
        List<ItemCarrinho> itens = validarItens(requisicao.itens());
        BigDecimal subtotal = itens.stream()
                .map(item -> Dinheiro.centavos(item.subtotal()))
                .reduce(ZERO, BigDecimal::add);
        BigDecimal pesoTotal = itens.stream()
                .map(ItemCarrinho::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        NivelClube nivel = niveis.buscar(requisicao.nivelClube())
                .orElseThrow(() -> new RegraNegocioException(NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.porCodigo(requisicao.regiao())
                .orElseThrow(() -> new RegraNegocioException(REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = modalidades.buscar(requisicao.modalidadeEntrega())
                .orElseThrow(() -> new RegraNegocioException(MODALIDADE_INVALIDA));
        if (!modalidade.atende(pesoTotal)) {
            throw new RegraNegocioException(MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = Dinheiro.centavos(nivel.aplicarFrete(modalidade.custo(pesoTotal)));

        ContextoCompra compra = new ContextoCompra(itens, subtotal, frete);
        BigDecimal desconto = calcularDesconto(requisicao.cupom(), compra);
        BigDecimal seguro = Dinheiro.percento(subtotal, regiao.percentualSeguro());
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = formasPagamento.buscar(requisicao.formaPagamento())
                .orElseThrow(() -> new RegraNegocioException(FORMA_PAGAMENTO_INVALIDA));
        int parcelas = requisicao.parcelas() == null ? 1 : requisicao.parcelas();
        if (parcelas < 1 || parcelas > forma.parcelasMaximas()) {
            throw new RegraNegocioException(PARCELAMENTO_INVALIDO);
        }
        if (!forma.disponivel(totalPedido)) {
            throw new RegraNegocioException(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Pagamento pagamento = forma.calcular(totalPedido, parcelas);

        return new ResumoCompra(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.valorFinal().subtract(totalPedido),
                pagamento.valorFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                Dinheiro.percento(subtotal, nivel.percentualCredito()),
                nivel.temBrinde(subtotal));
    }

    private static List<ItemCarrinho> validarItens(List<ItemRequisicao> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException(PEDIDO_INVALIDO);
        }
        return itens.stream().map(CalculadoraResumo::validarItem).toList();
    }

    private static ItemCarrinho validarItem(ItemRequisicao item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new RegraNegocioException(PEDIDO_INVALIDO);
        }
        return new ItemCarrinho(item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private BigDecimal calcularDesconto(String codigoCupom, ContextoCompra compra) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return ZERO;
        }
        Cupom cupom = cupons.buscar(codigoCupom)
                .orElseThrow(() -> new RegraNegocioException(CUPOM_INVALIDO));
        if (!cupom.aplicavel(compra)) {
            throw new RegraNegocioException(CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.desconto(compra));
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
