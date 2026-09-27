package com.novushq.coinlens.identify

import com.novushq.coinlens.common.AppResult

interface IdentifySpec<T> {
    val systemInstruction: String
    fun userPrompt(images: List<ImageInput>): String
    val schema: FieldSchema.Obj
    /** Lenient parse of the model's JSON; validates ranges and never throws for bad input. */
    fun parse(json: String): AppResult<T>
}

interface IdentifyEngine {
    suspend fun <T> identify(spec: IdentifySpec<T>, images: List<ImageInput>): AppResult<T>
}
