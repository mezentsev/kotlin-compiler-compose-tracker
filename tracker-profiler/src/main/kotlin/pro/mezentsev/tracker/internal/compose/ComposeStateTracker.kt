package pro.mezentsev.tracker.internal.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composer
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.State
import androidx.compose.runtime.cache
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateObject
import pro.mezentsev.tracker.internal.compose.StateObjectTrackManager.trackedStateChanges
import pro.mezentsev.tracker.internal.compose.StateObjectTrackManager.trackedStateObjects
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

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
        Timber.tag("TRACKER").i( "[Change] $composition: $change")
    }

    override fun forgotten(composition: StateObjectComposition) {
        Timber.tag("TRACKER").i( "[Forgotten] $composition")
    }

    override fun remembered(composition: StateObjectComposition, change: StateObjectChange) {
        Timber.tag("TRACKER").i("[Remembered] $composition: $change")
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
    val hash = state.asStateObject()?.hashCode() ?: state.hashCode()

    val register by lazy {
        object : RememberObserver {
            override fun onRemembered() {
                try {
                    val stateObject = state.asStateObject() ?: run {
                        Timber.tag("TRACKER").i("Error to remember $this")
                        return
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
                    Timber.tag("TRACKER").e(
                        unexpectedException,
                        "State value tracking registration failed",
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

    Timber.tag("TRACKER").d("Saved hash: $hash, $stateName, composableFunctionName: $composableFunctionName, fileName: $fileNameWithPackage")
    composer.startReplaceableGroup(hash)
    composer.cache(false) { register }
    composer.endReplaceableGroup()
}

private fun Any.asStateObject(): StateObject? {
    return when (this) {
        is StateObject -> this
        is Animatable<*, *> -> {
            val internalStateField = this::class.java.declaredFields.firstOrNull { field ->
                field.type == AnimationState::class.java
            }?.apply {
                isAccessible = true
            }
            val animationState = internalStateField?.get(this) as? AnimationState<*, *>
            animationState?.let { animationState::value.obtainStateObjectOrNull() }
        }
        is AnimationState<*, *> -> this::value.obtainStateObjectOrNull()
        is Transition<*>.TransitionAnimationState<*, *> -> this::value.obtainStateObjectOrNull()
        is InfiniteTransition.TransitionAnimationState<*, *> -> this::value.obtainStateObjectOrNull()
        else -> return null
    }
}

private fun KProperty0<*>.obtainStateObjectOrNull() = runCatching {
    val stateValue = apply { isAccessible = true }.getDelegate()
    stateValue as? StateObject
}.getOrNull()