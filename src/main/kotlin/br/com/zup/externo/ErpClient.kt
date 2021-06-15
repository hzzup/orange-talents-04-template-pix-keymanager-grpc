package br.com.zup.externo

import br.com.zup.servicosGrpc.Conta
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client

@Client("\${externo.erp}")
interface ErpClient {

    //consultando cliente por id, é retornado o cliente do ERP itau
    //metodo criado errado para criação da chave pix
    @Get(value = "/api/v1/clientes/{clienteId}", consumes = [MediaType.APPLICATION_JSON])
    fun consultaClienteId(@PathVariable("clienteId") clienteId : String?) : HttpResponse<ClientTitularResponse>

    @Get(value = "/api/v1/clientes/{clienteId}/contas{?tipo}", consumes = [MediaType.APPLICATION_JSON])
    fun consultaClienteConta(@PathVariable("clienteId") clienteId : String?,
                             @QueryValue("tipo") tipoConta : Conta) : HttpResponse<ClientTitularContaResponse>


}

//Classe que de fato virá como resposta para nós, com os dados do cliente
class ClientTitularResponse(val nome:String, val cpf:String, val instituicao:InstituicaoResponse)

//classe para retornar cliente+conta
class ClientTitularContaResponse(val tipo:Conta, val instituicao:InstituicaoResponse, val agencia:String,val numero:String, val titular : TitularResponse)

//classe para pegar os dados do titular
class TitularResponse(val id:String, val nome:String, val cpf:String)

//classe para pegarmos os dados da instituicao
class InstituicaoResponse(val nome:String, val ispb:String)