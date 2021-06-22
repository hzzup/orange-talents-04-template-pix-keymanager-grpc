package br.com.zup.integracao

import br.com.zup.BuscaChavePixRequest
import br.com.zup.KeyManagerBuscaChaveGrpcServiceGrpc
import br.com.zup.TipoDeChave
import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.Instituicoes
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

//subir contexto do micronaut no teste
@MicronautTest(transactional = false) //false pelo servico ser gRPC e não participar das mesmas transacoes
class BuscaChaveEndpointTest(
    @Inject val repository: ChavePixRepository, //necessario injetar meu repository para utilizar as funcoes do db
    @Inject val grpcClient: KeyManagerBuscaChaveGrpcServiceGrpc.KeyManagerBuscaChaveGrpcServiceBlockingStub
    //injetamos tambem o client gRPC que criamos para delegar sua funcao de registro
) {

    @Inject
    lateinit var bcbClient: BcbClient // (sistema externo bcb)

    lateinit var chaveNova : ChavePix

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
        //irei sempre salvar essa chave para ficar mais facil a utilizacao dela nos testes
        chaveNova = chave(br.com.zup.pix.TipoDeChave.EMAIL, "teste@teste.com.br", CLIENTE_ID)
        repository.save(chaveNova)
    }

    @Test //utilizar o padrão de cenario - > acao - > validacao
    fun `deve buscar uma chave por pixid e cliente id de uma chave cadastrada no nosso sistema`() {
        //cenario - "setup do ambiente para o teste"

        //acao - "realizar a tal acao de que será testada"
        //buscamos a chave com nosso cliente grpc
        val response = grpcClient.buscaChave(BuscaChavePixRequest.newBuilder()
            .setPixId(BuscaChavePixRequest.FiltroPorPixId.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setPixId(chaveNova.id.toString())
                .build())
            .build())

        //validacao - "validar o resultado"
        with(response) {
            //verificar se os campos trazidos são iguais aos salvos no banco (apenas alguns dados)
            assertEquals(CLIENTE_ID.toString(), clienteId)
            assertNotNull(pixId)
            //atenção nesse caso, pois o tipo de chave é igual ao enum do sistema externo
            //e não o enum do nosso sistema interno
            assertEquals(TipoDeChave.EMAIL,chave.tipo)
            assertEquals("teste@teste.com.br", chave.chave)
            assertEquals("60745840019",chave.conta.cpfDoTitular)
            assertEquals(Instituicoes.nome(ContaAssociada.ISPB),chave.conta.instituicao)
        }
    }

    @Test
    fun `deve buscar uma chave por valor da chave de uma chave cadastrada no nosso sistema`() {
        //cenario

        //acao
        //buscamos a chave com nosso cliente grpc
        val response = grpcClient.buscaChave(BuscaChavePixRequest.newBuilder()
            .setChave(chaveNova.chave)
            .build())

        //validacao
        with(response) {
            //verificar se os campos trazidos são iguais aos salvos no banco (apenas alguns dados)
            assertEquals(CLIENTE_ID.toString(), clienteId)
            assertNotNull(pixId)
            //atenção nesse caso, pois o tipo de chave é igual ao enum do sistema externo
            //e não o enum do nosso sistema interno
            assertEquals(TipoDeChave.EMAIL,chave.tipo)
            assertEquals("teste@teste.com.br", chave.chave)
            assertEquals("60745840019",chave.conta.cpfDoTitular)
            assertEquals(Instituicoes.nome(ContaAssociada.ISPB),chave.conta.instituicao)
        }
    }

    @Test
    fun `deve buscar uma chave por valor da chave de uma chave cadastrada no BCB`() {
        //cenario
        //mockamos a resposta vindo do BCB, como se ele tivesse encontrado a chave
        val mockResposta = PixKeyDetailsResponse(
            TipoChaveBcb.PHONE,
            "+5511222223333",
            contaBcb("60701190", "0001", "123456", TipoContaBcb.CACC),
            ClienteBcb(TipoCliente.NATURAL_PERSON, "Steve Jobs", "33059192057"),
            LocalDateTime.now()
        )
        Mockito.`when`(bcbClient.buscaChave("+5511222223333"))
            .thenReturn(HttpResponse.ok(mockResposta))

        //acao
        //buscamos a chave com nosso cliente grpc
        val response = grpcClient.buscaChave(BuscaChavePixRequest.newBuilder()
            .setChave("+5511222223333")
            .build())

        //validacao
        with(response) {
            //cliente id e pix id devem vir vazios já que não foram cadastrados do nosso sistema
            assertEquals("",clienteId)
            assertEquals("",pixId)
            //atenção nesse caso, pois o tipo de chave é igual ao enum do sistema externo
            //e não o enum do nosso sistema interno
            assertEquals(TipoDeChave.CELULAR,chave.tipo)
            assertEquals("+5511222223333", chave.chave)
            assertEquals("33059192057",chave.conta.cpfDoTitular)
            assertEquals(Instituicoes.nome(ContaAssociada.ISPB),chave.conta.instituicao)
        }
    }

    @Test
    fun `nao deve buscar uma chave quando cliente e pix id nao forem encontrados na nossa base`() {
        //cenario

        //acao
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.buscaChave(
                BuscaChavePixRequest.newBuilder()
                    .setPixId(
                        BuscaChavePixRequest.FiltroPorPixId.newBuilder()
                            .setClienteId(UUID.randomUUID().toString()) //UUIDS aleatorios que não existem na nossa base
                            .setPixId(UUID.randomUUID().toString())
                            .build()
                    )
                    .build()
            )
        }

        //validacao
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `nao deve buscar uma chave quando cliente e pix id forem invalidos`() {
        //cenario

        //acao
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.buscaChave(
                BuscaChavePixRequest.newBuilder()
                    .setPixId(
                        BuscaChavePixRequest.FiltroPorPixId.newBuilder()
                            .setClienteId("")
                            .setPixId("")
                            .build()
                    )
                    .build()
            )
        }

        //validacao
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve buscar uma chave quando o cliente ID nao for o mesmo dono da chave`() {
        //cenario
        val chaveClienteDiff = chave(
            br.com.zup.pix.TipoDeChave.EMAIL,
            "teste22222@teste.com.br",
            UUID.randomUUID()
        )
        repository.save(chaveClienteDiff)

        //acao
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.buscaChave(
                BuscaChavePixRequest.newBuilder()
                    .setPixId(
                        BuscaChavePixRequest.FiltroPorPixId.newBuilder()
                            .setClienteId(chaveClienteDiff.clienteId.toString())
                            .setPixId(chaveNova.id.toString())
                            .build()
                    )
                    .build()
            )
        }

        //validacao
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `nao deve buscar uma chave quando chave for invalida`() {
        //cenario

        //acao
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.buscaChave(
                BuscaChavePixRequest.newBuilder()
                    .setChave("")
                    .build()
            )
        }

        //validacao
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve buscar uma chave quando a chave nao for encontrada no nosso sitema nem no bcb`() {
        //cenario
        Mockito.`when`(bcbClient.buscaChave("+5511222223333"))
            .thenReturn(HttpResponse.notFound())

        //acao
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.buscaChave(
                BuscaChavePixRequest.newBuilder()
                    .setChave("+5511222223333")
                    .build()
            )
        }

        //validacao
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `nao deve buscar uma chave quando nao forem passados parametros esperados`(){
        //cenario

        //acao
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.buscaChave(
                BuscaChavePixRequest.newBuilder()
                    .build()
            )
        }

        //validacao
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix inválida ou não informada", status.description)
        }
    }

    //mockamos o servico HTTP BCB externo para não atrapalhar nos nossos testes
    @MockBean(BcbClient::class)
    fun bcbClient() : BcbClient {
        return Mockito.mock(BcbClient::class.java)
    }

    @Factory
    class Clients {
        @Bean //essa factory cria a comunicacao com nosso servico grpc (indicado pelo grpcserverchannel.name)
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel:ManagedChannel) : KeyManagerBuscaChaveGrpcServiceGrpc.KeyManagerBuscaChaveGrpcServiceBlockingStub {
            return KeyManagerBuscaChaveGrpcServiceGrpc.newBlockingStub(channel)
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