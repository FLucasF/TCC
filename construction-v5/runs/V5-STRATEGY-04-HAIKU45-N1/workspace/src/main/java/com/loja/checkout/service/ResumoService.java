package com.loja.checkout.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.model.Cupom;
import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.model.Item;
import com.loja.checkout.model.ModalidadeEntrega;
import com.loja.checkout.model.NivelClube;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.service.Resultado.Erro;
import com.loja.checkout.service.Resultado.Sucesso;
import com.loja.checkout.service.clube.EstrategiaClube;
import com.loja.checkout.service.clube.FactoryClube;
import com.loja.checkout.service.clube.ResultadoClube;
import com.loja.checkout.service.cupom.EstrategiaCupom;
import com.loja.checkout.service.cupom.FactoryCupom;
import com.loja.checkout.service.cupom.ResultadoCupom;
import com.loja.checkout.service.entrega.EstrategiaEntrega;
import com.loja.checkout.service.entrega.FactoryEntrega;
import com.loja.checkout.service.entrega.InfoEntrega;
import com.loja.checkout.service.pagamento.EstrategiaPagamento;
import com.loja.checkout.service.pagamento.FactoryPagamento;
import com.loja.checkout.service.pagamento.ResultadoPagamento;
import com.loja.checkout.service.validacao.ErroValidacao;
import com.loja.checkout.service.validacao.ValidadorPedido;

@Service
public class ResumoService {
    private final ValidadorPedido validador;
    private final SeguroService seguroService;

    public ResumoService(ValidadorPedido validador, SeguroService seguroService) {
        this.validador = validador;
        this.seguroService = seguroService;
    }

    public Resultado calcular(PedidoRequest pedido) {
        Optional<ErroValidacao> erroValidacao = validador.validar(pedido);
        if (erroValidacao.isPresent()) {
            return new Erro(erroValidacao.get().name());
        }

        try {
            // Converter DTOs para modelos
            List<Item> itens = pedido.itens().stream()
                .map(item -> new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
                .toList();

            ModalidadeEntrega modalidade = ModalidadeEntrega.valueOf(pedido.modalidadeEntrega());
            NivelClube nivel = NivelClube.valueOf(pedido.nivelClube());
            Regiao regiao = Regiao.valueOf(pedido.regiao());
            FormaPagamento forma = FormaPagamento.valueOf(pedido.formaPagamento());
            int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;

            // Calcular subtotal
            BigDecimal subtotal = calcularSubtotal(itens);

            // Aplicar cupom
            Cupom cupomEnum = null;
            if (pedido.cupom() != null && !pedido.cupom().isEmpty()) {
                cupomEnum = Cupom.valueOf(pedido.cupom());
            }

            // Calcular frete
            BigDecimal pesoTotal = calcularPesoTotal(itens);
            EstrategiaEntrega estrategiaEntrega = FactoryEntrega.criar(modalidade);
            InfoEntrega infoEntrega = estrategiaEntrega.calcular(pesoTotal);
            BigDecimal frete = Arredondamento.arredondar(infoEntrega.frete());
            int prazo = infoEntrega.prazo();

            // Aplicar desconto do cupom
            BigDecimal descontoCupom = BigDecimal.ZERO;
            BigDecimal freteComDesconto = frete;
            if (cupomEnum != null) {
                EstrategiaCupom estrategiaCupom = FactoryCupom.criar(cupomEnum);
                ResultadoCupom resultadoCupom = estrategiaCupom.aplicar(subtotal, frete, itens);
                descontoCupom = resultadoCupom.desconto();

                // Se o cupom oferece desconto de frete (FRETEGRATIS)
                if (resultadoCupom.freteDesconto().compareTo(BigDecimal.ZERO) > 0) {
                    freteComDesconto = BigDecimal.ZERO;
                }
            }

            // Aplicar nível de clube (desconto de frete para OURO)
            EstrategiaClube estrategiaClube = FactoryClube.criar(nivel);
            ResultadoClube resultadoClube = estrategiaClube.aplicar(subtotal);
            boolean temBrinde = resultadoClube.brinde();

            if (nivel == NivelClube.OURO) {
                freteComDesconto = BigDecimal.ZERO;
            }

            // Calcular seguro
            BigDecimal seguro = seguroService.calcular(subtotal, regiao);

            // Calcular total antes do ajuste de pagamento
            BigDecimal total = subtotal
                .subtract(descontoCupom)
                .add(freteComDesconto)
                .add(seguro);

            // Validar forma de pagamento com o total calculado
            if (forma == FormaPagamento.BOLETO && total.compareTo(new BigDecimal("1000.00")) > 0) {
                return new Erro(ErroValidacao.FORMA_PAGAMENTO_INDISPONIVEL.name());
            }

            // Aplicar ajuste de pagamento
            EstrategiaPagamento estrategiaPagamento = FactoryPagamento.criar(forma);
            if (!estrategiaPagamento.valido(total, parcelas)) {
                return new Erro(ErroValidacao.PARCELAMENTO_INVALIDO.name());
            }

            ResultadoPagamento resultadoPagamento = estrategiaPagamento.aplicar(total, parcelas);
            BigDecimal ajuste = resultadoPagamento.ajuste();
            int parcelasFinais = resultadoPagamento.parcelas();
            BigDecimal valorParcela = resultadoPagamento.valorParcela();

            // Calcular total final
            BigDecimal totalFinal = total.add(ajuste);

            // Calcular crédito do próximo
            BigDecimal credito = resultadoClube.credito();

            ResumoResponse resposta = new ResumoResponse(
                Arredondamento.arredondar(subtotal),
                Arredondamento.arredondar(descontoCupom),
                Arredondamento.arredondar(freteComDesconto),
                prazo,
                Arredondamento.arredondar(seguro),
                Arredondamento.arredondar(ajuste),
                Arredondamento.arredondar(totalFinal),
                parcelasFinais,
                valorParcela,
                Arredondamento.arredondar(credito),
                temBrinde
            );

            return new Sucesso(resposta);
        } catch (IllegalArgumentException e) {
            return new Erro(ErroValidacao.PEDIDO_INVALIDO.name());
        }
    }

    private BigDecimal calcularSubtotal(List<Item> itens) {
        return itens.stream()
            .map(item -> item.precoUnitario().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPesoTotal(List<Item> itens) {
        return itens.stream()
            .map(item -> item.pesoKg().multiply(new BigDecimal(item.quantidade())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
