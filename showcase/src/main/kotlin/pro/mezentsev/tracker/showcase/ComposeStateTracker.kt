package pro.mezentsev.tracker.showcase

import android.util.Log
import androidx.compose.runtime.Composer
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.State
import androidx.compose.runtime.cache
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateObject
import org.jetbrains.annotations.TestOnly
import pro.mezentsev.tracker.showcase.StateObjectTrackManager.stateFieldNameMap
import pro.mezentsev.tracker.showcase.StateObjectTrackManager.stateValueGetterMap
import pro.mezentsev.tracker.showcase.StateObjectTrackManager.trackedStateObjects
import java.util.concurrent.atomic.AtomicBoolean

data class StateValue(val previousValue: Any?, val newValue: Any?)

fun interface StateObjectGetter {
    operator fun invoke(state: Any): StateObject?
}

fun interface StateValueGetter {
    operator fun invoke(target: StateObject): StateValue
}

internal object StateObjectTrackManager {
    private val started = AtomicBoolean(false)
    private var previousHandle: ObserverHandle? = null

    internal val trackedStateObjects = mutableMapOf<String, MutableSet<StateObject>>()

    internal val stateFieldNameMap = mutableMapOf<StateObject, String>()
    internal val stateValueGetterMap = mutableMapOf<StateObject, StateValueGetter>()
    internal val stateLocationMap = mutableMapOf<StateObject, AffectedComposable>()

    internal fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            previousHandle = Snapshot.registerApplyObserver { stateObjects, _ ->
                stateObjects.forEach loop@{ stateObject ->
                    if (stateObject !is StateObject) return@loop

                    val name = stateFieldNameMap[stateObject] ?: return@loop
                    val value = stateValueGetterMap[stateObject]?.invoke(stateObject) ?: return@loop

                    println(
                        "TRACKER: $name changed from ${value.previousValue} to ${value.newValue}"
                    )
                }
            }
        }
    }
}

fun <State : Any> registerTracking(
    state: State,
    composer: Composer,
    composableKeyName: String,
    stateName: String,
    stateObjectGetter: StateObjectGetter = ComposeStateObjectGetter,
    stateValueGetter: StateValueGetter = ComposeStateObjectValueGetter,
): State = state.also {
    val register by lazy {
        object : RememberObserver {
            override fun onRemembered() {
                try {
                    println("TRACKER: Load onRemembered ${state.javaClass}")
                    val stateObject = stateObjectGetter(state) ?: return
                    println("TRACKER: Load: ${stateObject}")
                    trackedStateObjects.getOrPut(composableKeyName, ::mutableSetOf)
                        .add(stateObject)
                    stateFieldNameMap.putIfNotPresent(stateObject, stateName)
                    stateValueGetterMap.putIfNotPresent(stateObject, stateValueGetter)
                    ComposeStateObjectValueGetter.initialize(stateObject)
                } catch (unexpectedException: Exception) {
                    Log.e(
                        "TRACKER",
                        "State value tracking registration failed",
                        unexpectedException,
                    )
                }
            }

            override fun onForgotten() {
                // getOrDefault is available from API 24 (project minSdk is 21)
                trackedStateObjects[composableKeyName].orEmpty().forEach { state ->
                    println("TRACKER: Load onForgotten ${state.javaClass}")
                    stateFieldNameMap.remove(state)
                    stateValueGetterMap.remove(state)
                    ComposeStateObjectValueGetter.clean(state)
                }
                trackedStateObjects.remove(composableKeyName)
            }

            override fun onAbandoned() {}
        }
    }

    StateObjectTrackManager.ensureStarted()

    val hash = state.hashCode() + stateName.hashCode()
    println("TRACKER: Saved $hash ${state.javaClass}")
    composer.startReplaceableGroup(hash)
    composer.cache(true) { register }
    composer.endReplaceableGroup()
}

object ComposeStateObjectGetter : StateObjectGetter {
    override fun invoke(state: Any): StateObject? =
        when (state) {
            is StateObject -> state
            else -> null /* error("Unsupported state type: ${state::class.java}") */
        }
}

object ComposeStateObjectValueGetter : StateValueGetter {
    private val STATE_NO_VALUE = object {
        override fun toString() = "STATE_NO_VALUE"
    }

    private val stateValueMap = mutableMapOf<StateObject, StateValue>()

    private fun StateObject.getCurrentValue() = Snapshot.withoutReadObservation {
        (this as? State<*>
            ?: throw UnsupportedOperationException("Unsupported StateObject type: ${this::class.java}")).value
    }

    internal fun initialize(key: StateObject) {
        stateValueMap.putIfNotPresent(
            key,
            StateValue(previousValue = STATE_NO_VALUE, newValue = key.getCurrentValue()),
        )
    }

    internal fun clean(key: StateObject) {
        stateValueMap.remove(key)
    }

    internal fun clear() {
        stateValueMap.clear()
    }

    override fun invoke(target: StateObject): StateValue =
        StateValue(
            previousValue = stateValueMap[target]!!.newValue,
            newValue = target.getCurrentValue(),
        ).also { newValue ->
            stateValueMap[target] = newValue
        }
}


data class AffectedComposable(
    val name: String,
    val pkg: String,
    val filePath: String,
    val startLine: Int,
    val startColumn: Int,
) {
    public val fqName: String = pkg.takeUnless(String::isEmpty)?.plus(".").orEmpty() + name
}

internal fun <K, V> MutableMap<K, V>.putIfNotPresent(key: K, value: V) {
    if (!containsKey(key)) put(key, value)
}
