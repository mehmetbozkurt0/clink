package com.mehmetbozkurt.clink.core.mvi

import com.mehmetbozkurt.clink.testing.SequenceRandom
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreTest {
    @Test
    fun dispatch_appliesReducerInOrder() {
        val store = Store(0, Reducer<Int, Int> { s, a, _ -> s * 10 + a }, Random(0))
        store.dispatch(1)
        store.dispatch(2)
        assertEquals(12, store.state.value)
    }

    @Test
    fun dispatch_passesTheStoresRandom() {
        val store = Store(0, Reducer<Int, Unit> { _, _, r -> r.nextInt(100) }, SequenceRandom(42))
        store.dispatch(Unit)
        assertEquals(42, store.state.value)
    }
}
