package br.loja.checkout;

import br.loja.checkout.Carrinho.ItemCarrinho;
import br.loja.checkout.clube.NivelClube;
import br.loja.checkout.cupom.Cupom;
import br.loja.checkout.entrega.ModalidadeEntrega;
import br.loja.checkout.pagamento.FormaPagamento;
import br.loja.checkout.pagamento.FormaPagamento.Cobranca;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {
    private final Map<String, NivelClube> niveis;
    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, FormaPagamento> pagamentos;

    public ResumoService(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades,
                         List<Cupom> cupons, List<FormaPagamento> pagamentos) {
        this.niveis = porCodigo(niveis, NivelClube::codigo);
        this.modalidades = porCodigo(modalidades, ModalidadeEntrega::codigo);
        this.cupons = porCodigo(cupons, Cupom::codigo);
        this.pagamentos = porCodigo(pagamentos, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> porCodigo(List<T> lista, Function<T, String> codigo) {
        return lista.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }

    public ResumoResponse calcular(ResumoRequest req) {
        // As validações seguem a ordem definida pelo negócio.
        Carrinho carrinho = validarCarrinho(req.itens());
        NivelClube nivel = achar(niveis, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = Regiao.de(req.regiao()).orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));
        ModalidadeEntrega entrega = achar(modalidades, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!entrega.atende(carrinho)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : entrega.frete(carrinho);

        BigDecimal desconto = Dinheiro.ZERO;
        if (req.cupom() != null) {
            Cupom cupom = achar(cupons, req.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(carrinho)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            desconto = Dinheiro.arredondar(cupom.desconto(carrinho, frete));
        }

        FormaPagamento pagamento = achar(pagamentos, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = validarParcelas(req.parcelas(), pagamento);

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!pagamento.atende(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        Cobranca cobranca = pagamento.cobrar(totalPedido, parcelas);
        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                Dinheiro.percentual(subtotal, nivel.percentualCredito()),
                nivel.brinde(subtotal));
    }

    private static <T> T achar(Map<String, T> mapa, String codigo, String erro) {
        T valor = codigo == null ? null : mapa.get(codigo);
        if (valor == null) {
            throw new CheckoutException(erro);
        }
        return valor;
    }

    private static int validarParcelas(BigDecimal informado, FormaPagamento pagamento) {
        int parcelas = 1;
        if (informado != null) {
            if (informado.signum() <= 0 || informado.stripTrailingZeros().scale() > 0
                    || informado.compareTo(BigDecimal.valueOf(1000)) > 0) {
                throw new CheckoutException("PARCELAMENTO_INVALIDO");
            }
            parcelas = informado.intValueExact();
        }
        if (!pagamento.parcelasPermitidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        return parcelas;
    }

    private static Carrinho validarCarrinho(List<ResumoRequest.Item> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        List<ItemCarrinho> validados = new ArrayList<>();
        for (ResumoRequest.Item item : itens) {
            if (item == null || item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                    || item.pesoKg() == null || item.pesoKg().signum() <= 0
                    || item.quantidade() == null || item.quantidade().signum() <= 0
                    || item.quantidade().stripTrailingZeros().scale() > 0
                    || item.quantidade().compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            validados.add(new ItemCarrinho(item.nome(), item.precoUnitario(),
                    item.quantidade().intValueExact(), item.pesoKg()));
        }
        return Carrinho.de(validados);
    }
}
