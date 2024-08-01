package pro.mezentsev.tracker.internal.compose

import android.util.Log
import androidx.compose.runtime.Composer
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.State
import androidx.compose.runtime.cache
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateObject
import pro.mezentsev.tracker.internal.compose.StateObjectTrackManager.trackedStateChanges
import pro.mezentsev.tracker.internal.compose.StateObjectTrackManager.trackedStateObjects
import java.util.concurrent.atomic.AtomicBoolean

internal data class StateObjectComposition(
    val state: StateObject,
    val composableFunctionName: String,
    val stateName: String,
    val fileNameWithPackage: String,
) {
    override fun toString(): String = "$stateName [${fileNameWithPackage}#${composableFunctionName}]#${state.javaClass.simpleName}"
}

internal data class StateObjectChange(
    val prevValue: Any?,
    val newValue: Any?,
) {
    override fun toString(): String {
        return buildString {
            append("[")
            append(
                if (prevValue != null) {
                    "'$prevValue' -> "
                } else {
                    ""
                }
            )
            append("'$newValue']")
        }
    }
}

internal interface StateObjectChangeNotifier {
    fun changed(composition: StateObjectComposition, change: StateObjectChange)

    fun forgotten(composition: StateObjectComposition)

    fun remembered(composition: StateObjectComposition, change: StateObjectChange)
}

private val TRACKER_NOTIFIER = object : StateObjectChangeNotifier {
    override fun changed(composition: StateObjectComposition, change: StateObjectChange) {
        Log.i("TRACKER", "[Change] $composition: $change")
    }

    override fun forgotten(composition: StateObjectComposition) {
        Log.i("TRACKER", "[Forgotten] $composition")
    }

    override fun remembered(composition: StateObjectComposition, change: StateObjectChange) {
        Log.i("TRACKER", "[Remembered] $composition: $change")
    }
}

internal object StateObjectTrackManager {
    private val started = AtomicBoolean(false)
    private var observerHandler: ObserverHandle? = null

    internal val trackedStateObjects = mutableMapOf<Int, StateObjectComposition>()
    internal val trackedStateChanges = mutableMapOf<Int, StateObjectChange>()

    internal fun ensureStarted() {
        if (started.compareAndSet(false, true)) {
            observerHandler = Snapshot.registerApplyObserver { stateObjects, _ ->
                stateObjects.forEach loop@{ stateObject ->
                    if (stateObject !is StateObject) return@loop

                    val state = trackedStateObjects[stateObject.hashCode()] ?: return@loop
                    val oldChange = trackedStateChanges[stateObject.hashCode()] ?: return@loop

                    val newChange = oldChange.copy(
                        prevValue = oldChange.newValue,
                        newValue = (stateObject as? State<*>)?.value
                    )

                    trackedStateChanges[stateObject.hashCode()] = newChange

                    if (newChange.prevValue == null) {
                        TRACKER_NOTIFIER.remembered(state, newChange)
                    } else {
                        TRACKER_NOTIFIER.changed(state, newChange)
                    }
                }
            }
        }
    }
}

fun <S : Any> registerTracking(
    state: S,
    composer: Composer,
    composableFunctionName: String,
    stateName: String,
    fileNameWithPackage: String,
): S = state.also {
    val hash = state.hashCode()

    val register by lazy {
        object : RememberObserver {
            override fun onRemembered() {
                try {
                    val stateObject = when (state) {
                        is StateObject -> state
                        else -> return
                    }

                    val savedState = trackedStateObjects.getOrPut(hash) { StateObjectComposition(
                            stateObject,
                            composableFunctionName,
                            stateName,
                            fileNameWithPackage,
                        )
                    }

                    val savedChange = trackedStateChanges.compute(hash) { k, v ->
                        val rememberedValue = Snapshot.withoutReadObservation {
                            (stateObject as? State<*>)?.value
                        }

                        if (v == null || v.newValue != rememberedValue) {
                            StateObjectChange(
                                prevValue = v?.newValue,
                                newValue = rememberedValue
                            )
                        } else {
                            v
                        }
                    }

                    TRACKER_NOTIFIER.remembered(savedState, savedChange!!)
                } catch (unexpectedException: Exception) {
                    Log.e(
                        "TRACKER",
                        "State value tracking registration failed",
                        unexpectedException,
                    )
                }
            }

            override fun onForgotten() {
                trackedStateObjects[hash]?.let {
                    TRACKER_NOTIFIER.forgotten(it)
                }
                trackedStateObjects.remove(hash)
            }

            override fun onAbandoned() {}
        }
    }

    StateObjectTrackManager.ensureStarted()

    Log.d("TRACKER", "Saved hash: $hash, $stateName, composableFunctionName: $composableFunctionName, fileName: $fileNameWithPackage")
    composer.startReplaceableGroup(hash)
    composer.cache(false) { register }
    composer.endReplaceableGroup()
}
