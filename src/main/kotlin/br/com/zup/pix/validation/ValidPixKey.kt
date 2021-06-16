package br.com.zup.pix.validation

import br.com.zup.pix.registra.NovaChavePix
import javax.inject.Singleton
import javax.validation.Constraint
import javax.validation.Payload
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

@MustBeDocumented
@Target(CLASS, TYPE)
@Retention(RUNTIME)
@Constraint(validatedBy = [ValidPixKeyValidator::class])
annotation class ValidPixKey(
    val message: String = "chave Pix inválida (\${validatedValue.tipo})",
    val groups: Array<KClass<Any>> = [],
    val payload: Array<KClass<Payload>> = [],
)

//validacao do tipo da chave com o valor da chave
@Singleton
class ValidPixKeyValidator: javax.validation.ConstraintValidator<ValidPixKey, NovaChavePix> {

    override fun isValid(value: NovaChavePix?, context: javax.validation.ConstraintValidatorContext): Boolean {

        // must be validated with @NotNull
        if (value?.tipo == null) {
            return true
        }

        val valid = value.tipo.valida(value.chave)
        if (!valid) {
            // https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/#section-custom-property-paths
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate(context.defaultConstraintMessageTemplate) // or "chave Pix inválida (${value.tipo})"
                .addPropertyNode("chave").addConstraintViolation()
        }

        return value.tipo.valida(value.chave)
    }
}