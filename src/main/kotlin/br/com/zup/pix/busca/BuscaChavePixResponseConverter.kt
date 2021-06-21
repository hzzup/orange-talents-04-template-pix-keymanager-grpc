package br.com.zup.pix.busca

import br.com.zup.BuscaChavePixResponse
import br.com.zup.BuscaChavePixResponse.*
import br.com.zup.TipoDeChave
import br.com.zup.TipoDeConta
import com.google.protobuf.Timestamp
import java.time.ZoneId

//esse metodo extenso converte os detalhes de respostas vindo do bcb ou da minha aplicacao
//para o tipo de resposta esperada pelo grpc
class BuscaChavePixResponseConverter {
    fun convert(chaveInfo : ChavePixInfo) : BuscaChavePixResponse{
        return newBuilder()
            .setClienteId(chaveInfo.clienteId?.toString() ?: "") //busca tipo 1 retorna cliente id, tipo 2 não
            .setPixId(chaveInfo.pixId?.toString() ?: "")//busca tipo 1 retorna cliente id, tipo 2 não
            .setChave(ChavePix.newBuilder()
                .setTipo(TipoDeChave.valueOf(chaveInfo.tipo.name))
                .setChave(chaveInfo.chave)
                .setConta(ChavePix.ContaInfo.newBuilder()
                    .setTipo(TipoDeConta.valueOf(chaveInfo.tipoDeConta.name))
                    .setInstituicao(chaveInfo.conta.instituicao)
                    .setNomeDoTitular(chaveInfo.conta.nomeDoTitular)
                    .setCpfDoTitular(chaveInfo.conta.cpfDoTitular)
                    .setAgencia(chaveInfo.conta.agencia)
                    .setNumeroDaConta(chaveInfo.conta.numeroDaConta)
                    .build())
                .setCriadaEm(chaveInfo.registradaEm.let {
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(createdAt.epochSecond)
                        .setNanos(createdAt.nano)
                        .build()
                })
                .build())
            .build()
    }
}