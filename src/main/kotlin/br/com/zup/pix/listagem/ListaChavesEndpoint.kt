package br.com.zup.pix.listagem

import br.com.zup.KeyManagerListaChavesGrpcServiceGrpc
import br.com.zup.ListaChavesPixRequest
import br.com.zup.ListaChavesPixResponse
import br.com.zup.advice.ErrorHandler
import br.com.zup.pix.ChavePixRepository
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import java.lang.IllegalArgumentException
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@ErrorHandler
class ListaChavesEndpoint(@Inject val repository: ChavePixRepository) :
    KeyManagerListaChavesGrpcServiceGrpc.KeyManagerListaChavesGrpcServiceImplBase() {

    override fun listaChaves(request: ListaChavesPixRequest, responseObserver: StreamObserver<ListaChavesPixResponse>) {

        if(request.clienteId.isNullOrBlank()) {
            throw IllegalArgumentException("Cliente ID não pode ser nulo ou vazio!")
        }

        val chavesEncontradas = repository.findAllByClienteId(UUID.fromString(request.clienteId))
        val chavesResponse = chavesEncontradas.map {
            ListaChavesPixResponse.ChavePix.newBuilder()
                .setPixId(it.id.toString())
                .setTipoDeChave(br.com.zup.TipoDeChave.valueOf(it.tipo.name))
                .setChave(it.chave)
                .setTipoDeConta(br.com.zup.TipoDeConta.valueOf(it.tipoDeConta.name))
                .setCriadaEm(it.criadaEm.let {
                    val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder()
                        .setSeconds(createdAt.epochSecond)
                        .setNanos(createdAt.nano)
                        .build()
                })
                .build()
        }

        responseObserver.onNext(ListaChavesPixResponse.newBuilder()
            .setClienteId(request.clienteId)
            .addAllChaves(chavesResponse)
            .build())
        responseObserver.onCompleted()
    }
}

