package ru.unicorecms.unicoreconnect.common.events

import java.util.concurrent.CopyOnWriteArrayList

object EventDispatcher {
    private val handlers = CopyOnWriteArrayList<Pair<Class<*>, (Any) -> Unit>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> on(type: Class<T>, handler: (T) -> Unit) {
        handlers.add(type to handler as (Any) -> Unit)
    }

    inline fun <reified T : Any> on(noinline handler: (T) -> Unit) = on(T::class.java, handler)

    fun post(event: Any, onError: (Throwable) -> Unit = {}) {
        for ((type, handler) in handlers) {
            if (!type.isInstance(event)) continue

            try {
                handler(event)
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    fun clear() = handlers.clear()
}
