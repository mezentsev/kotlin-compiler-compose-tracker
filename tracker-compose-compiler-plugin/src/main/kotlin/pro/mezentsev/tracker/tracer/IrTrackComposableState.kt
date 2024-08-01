package pro.mezentsev.tracker.tracer

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
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
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
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class IrTrackComposableState(
    private val pluginContext: IrPluginContext,
    private val messageCollector: MessageCollector,
) {
    private val registerTrackerFunction = pluginContext.referenceFunctions(
        callableId = CallableId(
            FqName("pro.mezentsev.tracker.internal.compose"),
            Name.identifier("registerTracking")
        )
    ).single()

    fun irBuildBody(
        function: IrFunction,
        body: IrBody,
        currentFileName: String?
    ): IrBody {
        val statements = (body as? IrBlockBody)?.statements ?: return body

        return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
            statements.forEach { statement ->
                statement.accept(
                    StatementTransformer(
                        messageCollector,
                        this,
                        registerTrackerFunction,
                        function,
                        currentFileName
                    ),
                    null
                )
            }

            +statements
        }
    }

    private class StatementTransformer(
        private val messageCollector: MessageCollector,
        private val irBlockBodyBuilder: IrBlockBodyBuilder,
        private val registerTrackerFunction: IrSimpleFunctionSymbol,
        private val function: IrFunction,
        private val currentFileName: String?,
        private val parentVariables: MutableList<IrVariable> = mutableListOf(),
        private val depth: Int = 0
    ) : IrElementTransformerVoidWithContext() {
        private var currentStateVariables = mutableListOf<IrVariable>()

        override fun visitBlock(expression: IrBlock): IrExpression {
            messageCollector.report(
                CompilerMessageSeverity.LOGGING,
                "  ".repeat(depth) + "TRACKER: inspect block: ${expression.statements}"
            )
            return super.visitBlock(expression)
        }

        override fun visitVariable(declaration: IrVariable): IrStatement {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "  ".repeat(depth) + "TRACKER: found variable: ${declaration.name.asString()} is ${declaration.type.superTypes().map { it.classFqName?.asString() }}, ${declaration.symbol.owner.origin}"
            )

            if (declaration.type.isStateVariable() && declaration.symbol.owner.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE) {
                messageCollector.report(
                    CompilerMessageSeverity.WARNING,
                    "  ".repeat(depth) + "TRACKER: add variable: ${declaration.name.asString()} is ${declaration.type.superTypes().map { it.classFqName?.asString() }}, ${declaration.symbol.owner.origin}"
                )
                parentVariables.add(declaration)
            }

            return super.visitVariable(declaration)
        }

        override fun visitWhen(expression: IrWhen): IrExpression {
            expression.branches.forEach { branch ->
                val result = branch.result

                when (result) {
                    is IrCall -> messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "  ".repeat(depth) + "TRACKER: [result] found call: ${result.symbol.owner.fqNameWhenAvailable?.asString()}"
                    )

                    is IrConst<*> -> messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "  ".repeat(depth) + "TRACKER: [result] found const: ${result.value}"
                    )

                    is IrBlock -> {
                        messageCollector.report(
                            CompilerMessageSeverity.WARNING,
                            "  ".repeat(depth) + "TRACKER: [result] found block: ${result.statements}"
                        )

                        val newStatements = mutableListOf<IrStatement>()

                        result.statements.forEach { statement ->
                            statement.transformStatement(
                                StatementTransformer(
                                    messageCollector,
                                    irBlockBodyBuilder,
                                    registerTrackerFunction,
                                    function,
                                    currentFileName,
                                    currentStateVariables,
                                    depth + 1
                                )
                            )

                            newStatements.add(statement)
                        }

                        messageCollector.report(
                            CompilerMessageSeverity.WARNING,
                            "  ".repeat(depth) + "TRACKER: [variable] list to add: $currentStateVariables"
                        )

                        currentStateVariables.forEach {
                            irBlockBodyBuilder.irTrack(it)?.let { call ->
                                messageCollector.report(
                                    CompilerMessageSeverity.WARNING,
                                    "  ".repeat(depth) + "TRACKER: [variable] add new statement: ${it.type}"
                                )

                                newStatements.add(call)
                            }
                        }

                        currentStateVariables.clear()

                        result.statements.clear()
                        result.statements.addAll(newStatements)
                    }
                }
            }

            return super.visitWhen(expression)
        }

        private fun IrType.isStateVariable(): Boolean {
            return superTypes().any { it.classFqName?.asString()?.contains("State") == true }
        }

        private fun IrBlockBodyBuilder.irTrack(
            stateVariable: IrVariable
        ): IrExpression? {

            val composer = function.valueParameters.firstOrNull { it.name.asString() == "\$composer" }
                ?: run {
                    messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "Not found composer in variables: ${function.valueParameters.map { it.name.asString() }}"
                    )
                    return null
                }

            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Registered tracker for ${stateVariable.name.asString()}"
            )

            return irCall(registerTrackerFunction).also { call ->
                call.putValueArgument(0, irGet(stateVariable))
                call.putValueArgument(1, irGet(composer))
                call.putValueArgument(2, irString(function.name.asString()))
                call.putValueArgument(3, irString(stateVariable.name.asString()))
                call.putValueArgument(4, irString(currentFileName.orEmpty()))
            }
        }
    }
}
