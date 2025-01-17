package pro.mezentsev.tracker

import pro.mezentsev.tracker.compose.IrTrackComposableState
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.nameWithPackage
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

internal class TrackerComposeIrApplier(
    private val messageCollector: MessageCollector,
    pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {
    private var isComposable = false
    private var currentFileNameWithPackage: String? = null
    private val logger = object : Logger {
        override fun d(msg: String, depth: Int) {
            messageCollector.report(
                CompilerMessageSeverity.LOGGING,
                " ".repeat(depth) + "Tracker: " + msg
            )
        }

        override fun i(msg: String, depth: Int) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                " ".repeat(depth) + "Tracker: " + msg
            )
        }

        override fun e(msg: String, depth: Int) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                " ".repeat(depth) + "Tracker: " + msg
            )
        }
    }

    private val irTrackComposableState = IrTrackComposableState(pluginContext, logger)

    override fun visitFileNew(declaration: IrFile): IrFile {
        currentFileNameWithPackage = declaration.nameWithPackage
        val fileNew = super.visitFileNew(declaration)
        currentFileNameWithPackage = null
        return fileNew
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        val body = declaration.body
        if (body != null && declaration.hasAnnotation(ComposableFq)) {
            isComposable = true

            declaration.body = irTrackComposableState.irBuildBody(
                declaration,
                body,
                currentFileNameWithPackage
            )
        }

        val visitFunction = super.visitFunctionNew(declaration)
        isComposable = false
        return visitFunction
    }

    private companion object {
        private val ComposableFq = FqName("androidx.compose.runtime.Composable")
    }
}
