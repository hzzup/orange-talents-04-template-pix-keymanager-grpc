package br.com.zup.pix.listagem

import br.com.zup.BuscaChavePixRequest
import br.com.zup.KeyManagerListaChavesGrpcServiceGrpc
import br.com.zup.ListaChavesPixRequest
import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.Instituicoes
import br.com.zup.pix.TipoDeChave
import br.com.zup.pix.externo.*
import br.com.zup.pix.model.ChavePix
import br.com.zup.pix.model.ContaAssociada
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*

//subir contexto do micronaut no teste
@MicronautTest(transactional = false) //false pelo servico ser gRPC e não participar das mesmas transacoes
internal class ListaChaveEndpointTest(
    //injetamos tambem o client gRPC que criamos para delegar sua funcao de registro
    val grpcClient: KeyManagerListaChavesGrpcServiceGrpc.KeyManagerListaChavesGrpcServiceBlockingStub,
    val repository: ChavePixRepository //necessario injetar meu repository para utilizar as funcoes do db
) {

    //UUID gerado randomicamente que será utilizado no teste
    //como não conversamos diretamente com o servico HTTP externo, devemos gerar um UUID
    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    //por mais que estamos utilizando o h2, essa chamada virá antes de cada teste
    //ou seja o banco será limpo após cada um dos testes
    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test //utilizar o padrão de cenario - > acao - > validacao
    fun `deve buscar um cliente que nao possui chave e devolver uma lista vazia`() {
        //cenario - "setup do ambiente para o teste"

        //acao - "realizar a tal acao de que será testada"
        //buscamos a chave por cliente id com nosso cliente grpc
        val response = grpcClient.listaChaves(ListaChavesPixRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .build())

        //validacao - "validar o resultado"
        with(response) {
            //verificar se a resposta veio com uma lista de chavez vazia
            assertEquals(0,chavesCount)
            //alem de verificar se foi procurado pelo cliente passado
            assertEquals(CLIENTE_ID.toString(),clienteId)
        }
    }

    @Test
    fun `deve buscar as chaves de um cliente que possui todos os 4 tipos de chaves disponiveis`() {
        //cenario
        val valorAleatorio = UUID.randomUUID().toString()
        val chavesPix = mutableListOf<ChavePix>()
        chavesPix.add(chave(TipoDeChave.EMAIL, "teste@teste.com", CLIENTE_ID))
        chavesPix.add(chave(TipoDeChave.CPF, "37731203034", CLIENTE_ID))
        chavesPix.add(chave(TipoDeChave.ALEATORIA,valorAleatorio , CLIENTE_ID))
        chavesPix.add(chave(TipoDeChave.CELULAR, "+1122333334444", CLIENTE_ID))
        repository.saveAll(chavesPix)

        //acao
        //buscamos a chave por cliente id com nosso cliente grpc
        val response = grpcClient.listaChaves(ListaChavesPixRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .build())

        //validacao
        with(response) {
            assertEquals(4,chavesCount)
            assertEquals(br.com.zup.TipoDeChave.EMAIL, this.chavesList[0].tipoDeChave)
            assertEquals("teste@teste.com", this.chavesList[0].chave)
            assertEquals(br.com.zup.TipoDeChave.CPF, this.chavesList[1].tipoDeChave)
            assertEquals("37731203034", this.chavesList[1].chave)
            assertEquals(br.com.zup.TipoDeChave.ALEATORIA, this.chavesList[2].tipoDeChave)
            assertEquals(valorAleatorio, this.chavesList[2].chave)
            assertEquals(br.com.zup.TipoDeChave.CELULAR, this.chavesList[3].tipoDeChave)
            assertEquals("+1122333334444", this.chavesList[3].chave)
        }

    }

    @Test
    fun `nao deve buscar chave quando cliente id for invalido ou branco ou nulo`() {
        //cenario

        //acao
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.listaChaves(ListaChavesPixRequest.newBuilder()
                .setClienteId("")
                .build())
        }

        //validacao - "validar o resultado"
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code,status.code)
            assertEquals("Cliente ID não pode ser nulo ou vazio!", status.description)
        }
    }

    @Factory
    class Clients {
        @Bean //essa factory cria a comunicacao com nosso servico grpc (indicado pelo grpcserverchannel.name)
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel:ManagedChannel) : KeyManagerListaChavesGrpcServiceGrpc.KeyManagerListaChavesGrpcServiceBlockingStub {
            return KeyManagerListaChavesGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    //metodo para gerar uma chave rapidamente
    private fun chave(
        tipo: br.com.zup.pix.TipoDeChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID(),
    ): ChavePix {
        return ChavePix(
            clienteId = clienteId,
            tipo = tipo,
            chave = chave,
            tipoDeConta = br.com.zup.pix.TipoDeConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = Instituicoes.nome(ContaAssociada.ISPB),
                nomeDoTitular = "Teste",
                cpfDoTitular = "60745840019",
                agencia = "54",
                numeroDaConta = "123"
            )
        )
    }
}