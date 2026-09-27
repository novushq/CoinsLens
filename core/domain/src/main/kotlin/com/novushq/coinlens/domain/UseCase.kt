package com.novushq.coinlens.domain

import com.novushq.coinlens.common.AppResult

/**
 * Base contract for a unit of business logic. Use cases are pure Kotlin, take
 * their dependencies through the constructor, and are trivially unit testable.
 */
fun interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): AppResult<R>
}

fun interface NoParamUseCase<out R> {
    suspend operator fun invoke(): AppResult<R>
}
