package br.com.zup.pix.remove

import br.com.zup.KeyManagerRemoveGrpcServiceGrpc
import br.com.zup.RemoveChavePixRequest
import br.com.zup.RemoveChavePixResponse
import br.com.zup.advice.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RemoveChaveEndpoint(@Inject private val service : RemoveChavePixService)
    : KeyManagerRemoveGrpcServiceGrpc.KeyManagerRemoveGrpcServiceImplBase() {

    override fun remove(
        request: RemoveChavePixRequest,
        responseObserver: StreamObserver<RemoveChavePixResponse>
    ) {

        service.remove(request.pixId, request.clienteId)

        val chaveRemovida = RemoveChavePixResponse.newBuilder()
            .setClienteId(request.clienteId)
            .setPixId(request.pixId)
            .setMensagem("Chave excluida com sucesso")
            .build()

        responseObserver.onNext(chaveRemovida)
        responseObserver.onCompleted()
    }
}