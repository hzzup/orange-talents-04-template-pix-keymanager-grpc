package br.com.zup.advice.handlers

import br.com.zup.advice.ExceptionHandler
import br.com.zup.advice.ExceptionHandler.*
import br.com.zup.pix.exceptions.ChavePixExistenteException
import io.grpc.Status
import javax.inject.Singleton

@Singleton
class ChavePixExistenteExceptionHandler : ExceptionHandler<ChavePixExistenteException> {

    override fun handle(e: ChavePixExistenteException): StatusWithDetails {
        return StatusWithDetails(Status.ALREADY_EXISTS
            .withDescription(e.message)
            .withCause(e))
    }

    override fun supports(e: Exception): Boolean {
        return e is ChavePixExistenteException
    }
}