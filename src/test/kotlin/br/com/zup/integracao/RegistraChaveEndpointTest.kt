package br.com.zup.integracao

import br.com.zup.KeyManagerRegistraGrpcServiceGrpc
import br.com.zup.RegistraChavePixRequest
import br.com.zup.TipoDeChave
import br.com.zup.TipoDeConta
import br.com.zup.pix.ChavePixRepository
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
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

//subir contexto do micronaut no teste
@MicronautTest(transactional = false) //false pelo servico ser gRPC e não participar das mesmas transacoes
internal class RegistraChaveEndpointTest(
    //injetamos tambem o client gRPC que criamos para delegar sua funcao de registro
    val grpcClient: KeyManagerRegistraGrpcServiceGrpc.KeyManagerRegistraGrpcServiceBlockingStub,
    val repository: ChavePixRepository //necessario injetar meu repository para utilizar as funcoes do db
) {
    @Inject //injetamos uma variavel do servico HTTP externo que utilizamos
    lateinit var itauClient: ContasDeClientesNoItauClient
    @Inject
    lateinit var bcbClient: BcbClient // (agora dois itau+bcb)

    //objeto que irei inicializar para testar o bcb
    lateinit var bcbReq : CreatePixKeyRequest

    //UUID gerado randomicamente que será utilizado no teste
    //como não conversamos diretamente com o servico HTTP externo, devemos gerar um UUID
    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    //por mais que estamos utilizando o h2, essa chamada virá antes de cada teste
    //ou seja o banco será limpo após cada um dos testes
    @BeforeEach
    fun setup() {
        //bcbReq é o meu objeto que vou passar na requisicao ao BCB, como é o mesmo deixei no setup
        bcbReq = bcbReq(br.com.zup.pix.TipoDeChave.EMAIL, "teste@teste.com.br")
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
        `when`(bcbClient.cadastraChaveBcb(bcbReq))
            .thenReturn(HttpResponse.created(bcbResp()))

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

    @Test
    fun `nao deve registrar chave pix quando ocorrer erro no BCB`() {
        //cenario

        //ERP ITAU = OK, BCB = FALHOU
        `when`(itauClient.buscaContaPorTipo(clienteId = CLIENTE_ID.toString(), tipo ="CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))
        `when`(bcbClient.cadastraChaveBcb(bcbReq))
            .thenReturn(HttpResponse.unprocessableEntity())

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
            assertEquals("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)",status.description)
        }
    }



    //mockamos o servico HTTP ITAU externo para não atrapalhar nos nossos testes
    @MockBean(ContasDeClientesNoItauClient::class)
    fun itauClient() : ContasDeClientesNoItauClient {
        return Mockito.mock(ContasDeClientesNoItauClient::class.java)
    }

    //mockamos o servico HTTP BCB externo para não atrapalhar nos nossos testes
    @MockBean(BcbClient::class)
    fun bcbClient() : BcbClient {
        return Mockito.mock(BcbClient::class.java)
    }

    @Factory
    class Clients {
        @Bean //essa factory cria a comunicacao com nosso servico grpc (indicado pelo grpcserverchannel.name)
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel:ManagedChannel) : KeyManagerRegistraGrpcServiceGrpc.KeyManagerRegistraGrpcServiceBlockingStub {
            return KeyManagerRegistraGrpcServiceGrpc.newBlockingStub(channel)
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

    //metodo para gerar uma requisicao BCB rapidamente
    private fun bcbReq(tipo : br.com.zup.pix.TipoDeChave, chave : String) : CreatePixKeyRequest {
        return CreatePixKeyRequest(TipoChaveBcb.by(tipo),chave,bankAccount(),owner())
    }

    //metodo para gerar uma pix response (BCB)
    private fun bcbResp() : CreatePixKeyResponse {
        return CreatePixKeyResponse(
            bcbReq.keyType,
            bcbReq.key,
            bcbReq.bankAccount,
            bcbReq.owner,
            LocalDateTime.now()
        )
    }

    //metodo para gerar um owner (BCB)
    private fun owner(): ClienteBcb {
        return ClienteBcb(
            type = TipoCliente.NATURAL_PERSON,
            name = "Teste",
            taxIdNumber = "60745840019"
        )
    }

    //metodo para gerar um bank account (BCB)
    private fun bankAccount(): contaBcb {
        return contaBcb(
            participant = ContaAssociada.ISPB,
            branch = "54",
            accountNumber = "123",
            accountType = TipoContaBcb.by(br.com.zup.pix.TipoDeConta.CONTA_CORRENTE)
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