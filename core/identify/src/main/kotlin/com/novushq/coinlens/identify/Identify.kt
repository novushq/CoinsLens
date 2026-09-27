package com.novushq.coinlens.identify

/** Generic reusable Identify Engine contracts. No coin knowledge lives here. */

enum class ImageRole { PRIMARY, SECONDARY }

data class ImageInput(
    val bytes: ByteArray,
    val mimeType: String = "image/jpeg",
    val role: ImageRole = ImageRole.PRIMARY,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImageInput) return false
        return bytes.contentEquals(other.bytes) && mimeType == other.mimeType && role == other.role
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + role.hashCode()
        return result
    }
}

sealed interface FieldSchema {
    val description: String?
    val nullable: Boolean

    data class Obj(
        val properties: Map<String, FieldSchema>,
        val optional: List<String> = emptyList(),
        override val description: String? = null,
        override val nullable: Boolean = false,
    ) : FieldSchema

    data class Arr(
        val items: FieldSchema,
        val maxItems: Int? = null,
        override val description: String? = null,
        override val nullable: Boolean = false,
    ) : FieldSchema

    data class Str(
        override val description: String? = null,
        override val nullable: Boolean = false,
    ) : FieldSchema

    data class Enum(
        val values: List<String>,
        override val description: String? = null,
        override val nullable: Boolean = false,
    ) : FieldSchema

    data class Integer(
        override val description: String? = null,
        override val nullable: Boolean = false,
    ) : FieldSchema

    data class Number(
        override val description: String? = null,
        override val nullable: Boolean = false,
    ) : FieldSchema

    data class Bool(
        override val description: String? = null,
        override val nullable: Boolean = false,
    ) : FieldSchema
}
