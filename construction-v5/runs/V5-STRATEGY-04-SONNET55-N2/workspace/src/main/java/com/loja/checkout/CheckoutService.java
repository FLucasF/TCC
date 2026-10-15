package com.loja.checkout;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.SemCupom;
import com.loja.checkout.entrega.Entrega;
import com.loja.checkout.pagamento.Cobranca;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<Entrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> pagamentos;

    public CheckoutService(List<NivelClube> niveis, List<Entrega> entregas, List<Cupom> cupons,
            List<FormaPagamento> pagamentos) {
        this.niveis = new Catalogo<>(niveis, Erro.NIVEL_CLUBE_INVALIDO);
        this.entregas = new Catalogo<>(entregas, Erro.MODALIDADE_INVALIDA);
        this.cupons = new Catalogo<>(cupons, Erro.CUPOM_INVALIDO);
        this.pagamentos = new Catalogo<>(pagamentos, Erro.FORMA_PAGAMENTO_INVALIDA);
    }

    public ResumoResponse resumir(ResumoRequest req) {
        Pedido pedido = pedido(req.itens());
        NivelClube nivel = niveis.obter(req.nivelClube());
        Regiao regiao = regiao(req.regiao());
        Entrega entrega = entregas.obter(req.modalidadeEntrega());
        exigir(entrega.atende(pedido), Erro.MODALIDADE_INDISPONIVEL);
        Cupom cupom = req.cupom() == null ? SemCupom.INSTANCIA : cupons.obter(req.cupom());
        exigir(cupom.aplicavel(pedido), Erro.CUPOM_NAO_APLICAVEL);
        FormaPagamento pagamento = pagamentos.obter(req.formaPagamento());
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        exigir(pagamento.aceitaParcelas(parcelas), Erro.PARCELAMENTO_INVALIDO);

        BigDecimal subtotal = pedido.subtotal();
        BigDecimal frete = nivel.frete(entrega.frete(pedido));
        BigDecimal desconto = cupom.desconto(pedido, frete);
        BigDecimal seguro = Dinheiro.percentual(subtotal, regiao.taxaSeguro());
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);
        exigir(pagamento.disponivel(total), Erro.FORMA_PAGAMENTO_INDISPONIVEL);
        Cobranca cobranca = pagamento.cobrar(total, parcelas);

        return new ResumoResponse(subtotal, desconto, frete, entrega.prazoDias(), seguro,
                cobranca.totalFinal().subtract(total), cobranca.totalFinal(), parcelas,
                cobranca.valorParcela(), nivel.credito(subtotal), nivel.brinde(subtotal));
    }

    private static void exigir(boolean condicao, Erro erro) {
        if (!condicao) {
            throw new ErroNegocioException(erro);
        }
    }

    private static Regiao regiao(String codigo) {
        return Stream.of(Regiao.values())
                .filter(r -> r.name().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ErroNegocioException(Erro.REGIAO_INVALIDA));
    }

    private static Pedido pedido(List<ItemRequest> itens) {
        exigir(itens != null && !itens.isEmpty() && itens.stream().allMatch(CheckoutService::valido),
                Erro.PEDIDO_INVALIDO);
        return new Pedido(itens.stream()
                .map(i -> new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()))
                .toList());
    }

    private static boolean valido(ItemRequest i) {
        return Objects.nonNull(i)
                && i.precoUnitario() != null && i.precoUnitario().signum() > 0
                && i.quantidade() != null && i.quantidade() > 0
                && i.pesoKg() != null && i.pesoKg().signum() > 0;
    }
}
