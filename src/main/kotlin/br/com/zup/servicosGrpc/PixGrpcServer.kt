package br.com.zup.servicosGrpc

import br.com.zup.PixGrpcServiceGrpc
import br.com.zup.PixRequest
import br.com.zup.PixRequest.TipoChave
import br.com.zup.PixResponse
import br.com.zup.externo.ErpClient
import br.com.zup.servicosGrpc.validacoes.*
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton //anotacao para subir junto ao contexto micronaut
class PixGrpcServer(@Inject val erpClient : ErpClient, @Inject val pixRep : PixRepository) : PixGrpcServiceGrpc.PixGrpcServiceImplBase() {

    override fun cadastroChavePix(pixRequest: PixRequest?, responseObserver: StreamObserver<PixResponse>) {

        //Validacao para caso id cliente seja nulo ou vazio
        //retorno true significa que o cliente e invalido
        if (VerificaIdClienteInvalido(pixRequest?.idCliente)) {
            return retornoComErro(Status.INVALID_ARGUMENT, "Cliente invalido!",responseObserver)
        }

        //validacao para caso cliente nao exista no erp itau
        //retorno true significa que o cliente nao existe no erp
        //val cliente = erpClient.consultaClienteId(pixRequest?.idCliente) validacao errada
        val tipoDaConta = contaGrpcParaModelo(pixRequest?.tipoConta)
        val cliente = erpClient.consultaClienteConta(pixRequest?.idCliente,tipoDaConta)
        if(VerificaClienteErpItau(cliente.status)){
            return retornoComErro(Status.NOT_FOUND, "Cliente não encontrado!",responseObserver)
        }

        //validacao do tipo/valor da chave pix
        val verificaChave : Validado = VerificaTipoEValorChave(pixRequest?.tipoChave, pixRequest?.valorChave)
        if (verificaChave.semErro == false) {
            return retornoComErro(verificaChave.status,verificaChave.descricao,responseObserver)
        }

        //validacao do tipo da conta
        val verificaTipoConta : Validado = VerificaTipoConta(pixRequest?.tipoConta)
        if (verificaTipoConta.semErro == false) {
            return retornoComErro(verificaTipoConta.status, verificaTipoConta.descricao, responseObserver)
        }

        //verifico se é uma chava do tipo aleatoria, se sim, crio um UUID
        var valorFinalChave = pixRequest?.valorChave
        if (pixRequest?.tipoChave == TipoChave.ALEATORIA) {
            valorFinalChave = UUID.randomUUID().toString()
        }

        //verifico se existe alguma chave com o mesmo valor já salvo no banco
        if (pixRep.existsByValorChave(valorFinalChave!!)) {
            return retornoComErro(Status.ALREADY_EXISTS, "Chave já cadastrada!",responseObserver)
        }

        //criando minha chave para salvar no banco
        val clienteSemHttp = cliente.body()
        val novoPix = Pix(idCliente = pixRequest!!.idCliente,
                            agencia = clienteSemHttp.agencia,
                            numero = clienteSemHttp.numero,
                            nome= clienteSemHttp.titular.nome,
                            cpf= clienteSemHttp.titular.cpf,
                            instituicao= clienteSemHttp.instituicao.nome,
                            ispb = clienteSemHttp.instituicao.ispb,
                            conta= tipoDaConta,
                            tipoPix= chavePixGrpcParaModelo(pixRequest?.tipoChave),
                            valorChave = valorFinalChave!!
        )

        //salvando no banco
        try {
            pixRep.save(novoPix)
        } catch( e : Exception) {
            return retornoComErro(Status.UNKNOWN, "Erro inesperado + ${e.message}", responseObserver)
        }

        //retornando meu response e completando a requisição
        responseObserver.onNext(PixResponse.newBuilder().setPixId(novoPix.id!!).build())
        responseObserver.onCompleted()
    }

}

fun retornoComErro(status: Status, descricao: String, responseObserver: StreamObserver<PixResponse>?) {
    responseObserver?.onError(
        status
            .withDescription(descricao)
            .asRuntimeException()
    )
}