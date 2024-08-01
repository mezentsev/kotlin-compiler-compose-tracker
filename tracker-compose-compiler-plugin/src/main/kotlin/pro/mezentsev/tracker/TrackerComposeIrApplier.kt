package pro.mezentsev.tracker

import pro.mezentsev.tracker.tracer.IrTrackComposableState
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

internal class TrackerComposeIrApplier(
    messageCollector: MessageCollector,
    pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    private var isComposable = false

    private val irTrackComposableState = IrTrackComposableState(pluginContext, messageCollector)

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        val body = declaration.body
        if (body != null && declaration.hasAnnotation(ComposableFq)) {
            isComposable = true

            declaration.body = irTrackComposableState.irBuildBody(
                declaration,
                body
            )
        }

        val visitFunction = super.visitFunctionNew(declaration)
        isComposable = false
        return visitFunction
    }

    internal companion object {
        val ComposableFq = FqName("androidx.compose.runtime.Composable")
    }
}
