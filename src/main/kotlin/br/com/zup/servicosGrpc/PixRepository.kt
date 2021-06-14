package br.com.zup.servicosGrpc

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface PixRepository : JpaRepository<Pix, Long> {

    //metodo para verificar se ja existe uma chave com tal valor no banco
    fun existsByValorChave(valorChave : String) : Boolean

}