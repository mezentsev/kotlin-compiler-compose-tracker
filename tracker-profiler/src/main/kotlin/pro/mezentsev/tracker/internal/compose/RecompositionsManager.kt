package pro.mezentsev.tracker.internal.compose

import androidx.compose.runtime.Composer
import timber.log.Timber

internal data class ObjectRecomposition(
    val composableFunctionName: String,
    val fileNameWithPackage: String,
) {
    override fun toString(): String =
        "$fileNameWithPackage#$composableFunctionName"
}

internal interface RecompositionNotifier {
    fun recomposed(recomposition: ObjectRecomposition)

    fun skipped(recomposition: ObjectRecomposition)
}

internal val RECOMPOSITIONS_NOTIFIER = object : RecompositionNotifier {
    override fun recomposed(recomposition: ObjectRecomposition) {
        Timber.tag("TRACKER").i("[Recomposed]: $recomposition")
    }

    override fun skipped(recomposition: ObjectRecomposition) {
        Timber.tag("TRACKER").i("[Skip]: $recomposition")
    }
}

fun notifyRecomposition(
    composer: Composer,
    composableFunctionName: String,
    fileNameWithPackage: String,
) {
    RECOMPOSITIONS_NOTIFIER.recomposed(
        ObjectRecomposition(
            composableFunctionName,
            fileNameWithPackage
        )
    )
}

fun notifySkip(
    composer: Composer,
    composableFunctionName: String,
    fileNameWithPackage: String,
) {
    RECOMPOSITIONS_NOTIFIER.skipped(
        ObjectRecomposition(
            composableFunctionName,
            fileNameWithPackage
        )
    )
}