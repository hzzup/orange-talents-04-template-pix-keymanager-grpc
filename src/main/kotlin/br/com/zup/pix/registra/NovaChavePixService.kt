package br.com.zup.pix.registra

import br.com.zup.pix.ChavePixRepository
import br.com.zup.pix.exceptions.ChavePixExistenteException
import br.com.zup.pix.externo.BcbClient
import br.com.zup.pix.externo.ContasDeClientesNoItauClient
import br.com.zup.pix.externo.CreatePixKeyRequest
import br.com.zup.pix.model.ChavePix
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated // essa classe será validada de acordo com as anotacoes do dto
@Singleton
class NovaChavePixService(@Inject val repository: ChavePixRepository,
                          @Inject val itauClient: ContasDeClientesNoItauClient,
                          @Inject val bcbClient: BcbClient
) {

    @Transactional //transactional pois salvará nossa chave pix no banco
    //retorna nossa entidade modelo de chave pix
    fun registra(@Valid novaChave: NovaChavePix): ChavePix {

        // Verifica se chave já existe no sistema, se ja existe, throws exception
        if (repository.existsByChave(novaChave.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChave.chave}' existente")

        // Busca dados da conta no ERP do ITAU - se não retornar um body, requisicao falhou, throws exception
        val response = itauClient.buscaContaPorTipo(novaChave.clienteId!!, novaChave.tipoDeConta!!.name)
        val conta = response.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itau")

        // Grava no banco de dados
        val chave = novaChave.toModel(conta)
        repository.save(chave)

        //devemos salvar no BCB agora, porém antes convertemos nossa chave
        //para o modelo de chave esperado pelo BCB
        val bcbRequest = CreatePixKeyRequest.of(chave)

        //agora de fato iremos realizar a requisicao para o BCB
        //precisamos verificar se salvou corretamente ou não
        //201 created (sucesso) ou 422 unprocessable entity (falha)
        val bcbResponse = bcbClient.cadastraChaveBcb(bcbRequest)
        if (bcbResponse.status != HttpStatus.CREATED) {
            throw IllegalStateException("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)")
        }

        //como o BCB que irá gerar nossas chaves aleatorias agora
        //precisamos verificar se ela é do tipo aleatoria e pegar o valor vindo do bcb
        chave.atualizaChaveAleatoria(bcbResponse.body().key)

        return chave
    }
}