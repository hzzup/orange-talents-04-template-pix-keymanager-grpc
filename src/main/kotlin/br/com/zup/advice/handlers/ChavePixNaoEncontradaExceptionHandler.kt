package br.com.zup.advice.handlers

import br.com.zup.advice.ExceptionHandler
import br.com.zup.advice.ExceptionHandler.*
import br.com.zup.pix.exceptions.ChavePixNaoEncontradaException
import io.grpc.Status
import javax.inject.Singleton

@Singleton
class ChavePixNaoEncontradaExceptionHandler : ExceptionHandler<ChavePixNaoEncontradaException> {

    override fun handle(e: ChavePixNaoEncontradaException): StatusWithDetails {
        return StatusWithDetails(Status.NOT_FOUND
            .withDescription(e.message)
            .withCause(e))
    }

    override fun supports(e: Exception): Boolean {
        return e is ChavePixNaoEncontradaException
    }
}