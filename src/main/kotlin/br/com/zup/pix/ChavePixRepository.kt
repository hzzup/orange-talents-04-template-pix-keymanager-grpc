package br.com.zup.pix

import br.com.zup.pix.model.ChavePix
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository : JpaRepository<ChavePix, UUID> {

    fun existsByChave(chave: String?): Boolean

    fun findByIdAndClienteId(id : UUID, clienteId : UUID) : Optional<ChavePix>

}