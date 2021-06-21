package br.com.zup.pix.model

import br.com.zup.pix.TipoDeChave
import br.com.zup.pix.TipoDeConta
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Entity
@Table(uniqueConstraints = [UniqueConstraint(
    name = "uk_chave_pix",
    columnNames = ["chave"]
)])
class ChavePix(
    @field:NotNull
    @Column(nullable = false)
    val clienteId: UUID,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipo: TipoDeChave,

    @field:NotBlank
    @Column(unique = true, nullable = false)
    var chave: String,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipoDeConta: TipoDeConta,

    @field:Valid
    @Embedded
    val conta: ContaAssociada
) {
    @Id
    @GeneratedValue
    val id: UUID? = null

    @Column(nullable = false)
    val criadaEm: LocalDateTime = LocalDateTime.now()

    //atualiza chave com o valor vindo do bcb quando é aleatoria
    fun atualizaChaveAleatoria(chaveBcb: String) : Boolean {
        if (tipo == TipoDeChave.ALEATORIA) {
            this.chave = chaveBcb
            return true
        }
        return false
    }

    //verifica por uma chave se o cliente é o mesmo de tal chave
    fun chavePertenceAo(clienteId: UUID) = this.clienteId.equals(clienteId)

}
