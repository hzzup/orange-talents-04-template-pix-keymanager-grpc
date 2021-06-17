package br.com.zup.integracao

import br.com.zup.KeymanagerRegistraGrpcServiceGrpc
import br.com.zup.RegistraChavePixRequest
import br.com.zup.TipoDeChave
import br.com.zup.TipoDeConta
import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.externo.ContasDeClientesNoItauClient
import br.com.zup.pix.externo.DadosDaContaResponse
import br.com.zup.pix.externo.InstituicaoResponse
import br.com.zup.pix.externo.TitularResponse
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
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.lang.RuntimeException
import java.util.*
import javax.inject.Inject

//subir contexto do micronaut no teste
@MicronautTest(transactional = false) //false pelo servico ser gRPC e não participar das mesmas transacoes
class RegistraChaveEndpointTest(
    @Inject val repository: ChavePixRepository, //necessario injetar meu repository para utilizar as funcoes do db
    @Inject val grpcClient: KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub
    //injetamos tambem o client gRPC que criamos para delegar sua funcao de registro
) {
    @Inject //injetamos uma variavel do servico HTTP externo que utilizamos
    lateinit var itauClient: ContasDeClientesNoItauClient

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
    fun `deve registrar nova chave pix`() {
        //cenario - "setup do ambiente para o teste"
        //when deve ser usado entre `` para não ser utilizado a palavra reservada do kotlin "when"
        //no nosso mock quando pedir para buscar conta por tipo, nós mockamos um resultado
        //para o UUID que criamos e um tipo de conta e retornamos um resultado de cliente OK!
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo ="CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        //acao - "realizar a tal acao de que será testada"
        //registramos a nova chave (Criando ela diretamente no parametro)
        val response = grpcClient.registra(RegistraChavePixRequest.newBuilder()
            .setClienteId(CLIENTE_ID.toString())
            .setTipoDeChave(TipoDeChave.EMAIL)
            .setChave("teste@teste.com.br")
            .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
            .build())

        //validacao - "validar o resultado"
        with(response) {
            //verificar se o cliente que passamos foi o mesmo que foi registrado
            assertEquals(CLIENTE_ID.toString(), clienteId)
            //verificar se foi gerado o pixId no banco
            assertNotNull(pixId)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando chave ja existe`() {
        //cenario
        //primeiro precisamos criar uma chave no banco
        val chaveSalva = repository.save(
            chave(
                tipo = br.com.zup.pix.TipoDeChave.CPF,
                chave = "60745840019",
                clienteId = CLIENTE_ID
            )
        )

        //acao
        //devemos verificar se recebemos uma runtime exception (chave ja existente)
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.registra(RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoDeChave(TipoDeChave.CPF)
                .setChave("60745840019")
                .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
                .build())
        }

        //validacao
        //aqui com nosso retorno da excecao, validamos se o status de retorno é igual ao ALREADY_EXISTS
        //alem de verificarmos se a descricao bate com a descricao recebida
        with(thrown) {
            assertEquals(Status.ALREADY_EXISTS.code,status.code)
            assertEquals("Chave Pix '${chaveSalva.chave}' existente",status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando nao encontrar dados no servico ERP itau`() {
        //cenario
        //mockamos uma resposta que nos retornaria 404, cliente não encontrado no erp itau
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(),tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.notFound())

        //acao
        //pegamos a excecao que é jogada, ja que não conseguimos recuperar um cliente do erp itau
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.registra(RegistraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoDeChave(TipoDeChave.EMAIL)
                .setChave("teste@teste.com.br")
                .setTipoDeConta(TipoDeConta.CONTA_CORRENTE)
                .build())
        }

        //validacao
        //verificamos se os codigos de erro e descricao batem
        with(thrown) {
            assertEquals(Status.FAILED_PRECONDITION.code,status.code)
            assertEquals("Cliente não encontrado no Itau",status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix quando parametros forem invalidos`() {
        //cenario
        //chave vazia sem nenhum dado preenchido
        val chaveInvalida = RegistraChavePixRequest.newBuilder().build()

        //acao
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.registra(chaveInvalida)
        }

        //validacao
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code,status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    //mockamos o servico HTTP externo para não atrapalhar nos nossos testes
    @MockBean(ContasDeClientesNoItauClient::class)
    fun itauClient() : ContasDeClientesNoItauClient {
        return Mockito.mock(ContasDeClientesNoItauClient::class.java)
    }

    @Factory
    class Clients {
        @Bean //essa factory cria a comunicacao com nosso servico grpc (indicado pelo grpcserverchannel.name)
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel:ManagedChannel) : KeymanagerRegistraGrpcServiceGrpc.KeymanagerRegistraGrpcServiceBlockingStub {
            return KeymanagerRegistraGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    //criamos esse metodo para gerar uma response qualquer para conseguirmos testar sem depender do sistema externo
    private fun dadosDaContaResponse(): DadosDaContaResponse {
        return DadosDaContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse("TESTE", ContaAssociada.ISPB),
            agencia = "54",
            numero = "123",
            titular = TitularResponse("Teste", "60745840019")
        )
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
                instituicao = "TESTE",
                nomeDoTitular = "Teste",
                cpfDoTitular = "60745840019",
                agencia = "54",
                numeroDaConta = "123"
            )
        )
    }
}