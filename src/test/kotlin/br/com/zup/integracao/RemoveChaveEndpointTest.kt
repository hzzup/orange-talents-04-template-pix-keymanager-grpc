package br.com.zup.integracao

import br.com.zup.*
import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.model.ChavePix
import br.com.zup.pix.model.ContaAssociada
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import javax.inject.Inject

//subir contexto do micronaut no teste
@MicronautTest(transactional = false) //false pelo servico ser gRPC e não participar das mesmas transacoes
class RemoveChaveEndpointTest(
    @Inject val repository: ChavePixRepository, //necessario injetar meu repository para utilizar as funcoes do db
    @Inject val grpcClient: KeyManagerRemoveGrpcServiceGrpc.KeyManagerRemoveGrpcServiceBlockingStub
    //injetamos tambem o client gRPC que criamos para delegar sua funcao de registro
) {

    lateinit var CHAVE_SALVA : ChavePix

    //UUID gerado randomicamente que será utilizado no teste
    //como não conversamos diretamente com o servico HTTP externo, devemos gerar um UUID
    companion object {
        val CLIENTE_ID = UUID.randomUUID()
        val INVALID_PIX_ID = UUID.randomUUID()
    }

    //chave salva para testar a delecao de chaves
    @BeforeEach
    fun setup() {
        CHAVE_SALVA = repository.save(
            chave(
                tipo = br.com.zup.pix.TipoDeChave.CPF,
                chave = "60745840019",
                clienteId = CLIENTE_ID
            )
        )
    }

    //rotina de limpeza após cada teste
    @AfterEach
    fun cleanup() {
        repository.deleteAll()
    }

    @Test //utilizar o padrão de cenario - > acao - > validacao
    fun `deve deletar uma chave pix`() {
        //cenario - "setup do ambiente para o teste"
        //primeiro eu preciso cadastrar uma chave para poder deletar

        //acao - "realizar a tal acao de que será testada"
        //apagamos a mesma chave que acabamos de salvar
        val chaveRemovida = grpcClient.remove(
            RemoveChavePixRequest.newBuilder()
                .setClienteId(CHAVE_SALVA.clienteId.toString())
                .setPixId(CHAVE_SALVA.id.toString())
                .build()
        )

        //validacao - "validar o resultado"
        //verificamos se temos algum registro no banco (vale ressaltar que o banco é limpo a cada teste)
        assertEquals(repository.count(),0)
        assertEquals(CHAVE_SALVA.id.toString(),chaveRemovida.pixId)
        assertEquals(CHAVE_SALVA.clienteId.toString(),chaveRemovida.clienteId)
        assertEquals("Chave excluida com sucesso",chaveRemovida.mensagem)
    }

    @Test
    fun `nao deve remover chave pix quando chave nao existe`() {
        //cenario

        //acao
        //devemos verificar se recebemos uma runtime exception (chave nao existente)
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(RemoveChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setPixId(INVALID_PIX_ID.toString())
                .build())
        }

        //validacao
        //aqui com nosso retorno da excecao, validamos se o status de retorno é igual ao NOT_FOUND
        //alem de verificarmos se a descricao bate com a descricao recebida
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code,status.code)
            assertEquals("Chave pix nao encontrado ou nao associada a um cliente",status.description)
        }
    }

    @Test
    fun `nao deve remover chave pix quando parametros forem invalidos`() {
        //cenario
        val chaveInvalida = RemoveChavePixRequest.newBuilder().build()

        //acao
        //excecao lancada ao tentar argumentos invalidos
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(chaveInvalida)
        }

        //validacao
        //verificamos se os codigos de erro e descricao batem
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code,status.code)
            assertEquals("Dados inválidos",status.description)
        }
    }

    @Test
    fun `nao deve remover chave pix quando existente porem pertence a outro cliente`() {
        //cenario
        val chaveOutroCliente = repository.save(
            chave(
                tipo = br.com.zup.pix.TipoDeChave.CPF,
                chave = "65620999009",
                clienteId = UUID.randomUUID()
            )
        )

        //acao
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(
                RemoveChavePixRequest.newBuilder()
                    .setPixId(CHAVE_SALVA.id.toString())
                    .setClienteId(chaveOutroCliente.clienteId.toString())
                    .build()
            )
        }

        //validacao
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code,status.code)
            assertEquals("Chave pix nao encontrado ou nao associada a um cliente",status.description)
        }
    }

    @Factory
    class Clients {
        @Bean //essa factory cria a comunicacao com nosso servico grpc (indicado pelo grpcserverchannel.name)
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel:ManagedChannel) : KeyManagerRemoveGrpcServiceGrpc.KeyManagerRemoveGrpcServiceBlockingStub {
            return KeyManagerRemoveGrpcServiceGrpc.newBlockingStub(channel)
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
                instituicao = "TESTE",
                nomeDoTitular = "Teste",
                cpfDoTitular = "60745840019",
                agencia = "54",
                numeroDaConta = "123"
            )
        )
    }
}