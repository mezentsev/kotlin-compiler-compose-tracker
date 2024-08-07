package pro.mezentsev.tracker.compose

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import pro.mezentsev.tracker.Logger

internal class IrTrackComposableState(
    private val pluginContext: IrPluginContext,
    private val logger: Logger,
) {
    private val registerTrackerFunction = pluginContext.referenceFunctions(
        callableId = CallableId(
            FqName("pro.mezentsev.tracker.internal.compose"),
            Name.identifier("registerTracking")
        )
    ).single()
    private val trackerFunction = pluginContext.referenceFunctions(
        callableId = CallableId(
            FqName("pro.mezentsev.tracker.internal.compose"),
            Name.identifier("registerTracking")
        )
    ).single()

    private val recomposeNotifyFunction = pluginContext.referenceFunctions(
        callableId = CallableId(
            FqName("pro.mezentsev.tracker.internal.compose"),
            Name.identifier("notifyRecomposition")
        )
    ).single()

    private val skipNotifyFunction = pluginContext.referenceFunctions(
        callableId = CallableId(
            FqName("pro.mezentsev.tracker.internal.compose"),
            Name.identifier("notifySkip")
        )
    ).single()

    private val logFunction = pluginContext.referenceFunctions(
        callableId = CallableId(
            FqName("kotlin.io"),
            Name.identifier("println")
        )
    ).single {
        val parameters = it.owner.valueParameters
        parameters.size == 1 && parameters[0].type == pluginContext.irBuiltIns.anyNType
    }

    fun irBuildBody(
        function: IrFunction,
        body: IrBody,
        currentFileName: String?
    ): IrBody {
        val statements = (body as? IrBlockBody)?.statements ?: return body

        return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
            statements.forEach { statement ->
                statement.accept(
                    ComposeStatementTransformer(
                        logger,
                        this,
                        trackerFunction,
                        logFunction,
                        recomposeNotifyFunction,
                        skipNotifyFunction,
                        function,
                        currentFileName
                    ),
                    null
                )

                logger.d(statement.dump())
            }

            +statements
        }
    }
}
