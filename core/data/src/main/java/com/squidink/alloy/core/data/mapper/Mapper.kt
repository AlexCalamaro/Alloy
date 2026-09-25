package com.squidink.alloy.core.data.mapper

/**
 * Functional interface for mapping between types.
 *
 * Used in repository layer to convert between data models and domain models.
 *
 * @param In The input type
 * @param Out The output type
 */
fun interface DataMapper<in In, out Out> {
    /**
     * Map an input object to an output object.
     *
     * @param input The input object
     * @return The mapped output object
     */
    fun map(input: In): Out
}

/**
 * Extension function to create a DataMapper from a lambda.
 */
fun <In, Out> mapper(transform: (In) -> Out): DataMapper<In, Out> = DataMapper(transform)

/**
 * Composable mapper that chains multiple transformations.
 *
 * @param other The next mapper in the chain
 * @return A new mapper that applies this mapper then the other
 */
infix fun <A, B, C> DataMapper<A, B>.andThen(other: DataMapper<B, C>): DataMapper<A, C> {
    return DataMapper { other.map(this.map(it)) }
}

/**
 * Bidirectional mapper for two-way conversion.
 *
 * @param A The forward direction type
 * @param B The reverse direction type
 */
data class BiMapper<A, B>(
    val toB: (A) -> B,
    val toA: (B) -> A
) {
    val forwardMapper: DataMapper<A, B> = DataMapper(toB)
    val reverseMapper: DataMapper<B, A> = DataMapper(toA)
}

/**
 * Extension function to create a BiMapper from two lambdas.
 */
fun <A, B> biMapper(forward: (A) -> B, reverse: (B) -> A): BiMapper<A, B> {
    return BiMapper(forward, reverse)
}
