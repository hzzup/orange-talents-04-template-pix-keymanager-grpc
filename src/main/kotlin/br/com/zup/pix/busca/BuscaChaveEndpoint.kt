package br.com.zup.pix.busca

import br.com.zup.BuscaChavePixRequest
import br.com.zup.BuscaChavePixResponse
import br.com.zup.KeyManagerBuscaChaveGrpcServiceGrpc
import br.com.zup.advice.ErrorHandler
import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.externo.BcbClient
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

@Singleton
@ErrorHandler
class BuscaChaveEndpoint(@Inject val repository: ChavePixRepository,
                         @Inject val bcbClient: BcbClient,
                         @Inject private val validator: Validator
) : KeyManagerBuscaChaveGrpcServiceGrpc.KeyManagerBuscaChaveGrpcServiceImplBase() {

    override fun buscaChave(request: BuscaChavePixRequest, responseObserver: StreamObserver<BuscaChavePixResponse>) {

        //aqui estou transformando a minha classe no tipo de filtro especifico para a requisicao
        //verificar o oneof do proto para mais informações dos tipos de pesquisa que temos
        val filtro = request.toModel(validator)

        //com o filtro em mãos aplicamos seu metodo abstrato implementado por cada filtro diferente
        //o metodo filtra irá validar as informações, se tudo estiver ok, temos agora as informações da chave
        val chaveInfo = filtro.filtra(repository,bcbClient)

        responseObserver.onNext(BuscaChavePixResponseConverter().convert(chaveInfo))
        responseObserver.onCompleted()
    }

}