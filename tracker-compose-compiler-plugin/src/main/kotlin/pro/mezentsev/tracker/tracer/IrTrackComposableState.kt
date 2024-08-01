@file:OptIn(UnsafeCastFunction::class)

package pro.mezentsev.tracker.tracer

import pro.mezentsev.tracker.TrackerComposeIrApplier.Companion.ComposableFq
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
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class IrTrackComposableState(
    private val pluginContext: IrPluginContext,
    private val messageCollector: MessageCollector,
) {
    private val setOfVariables = mutableSetOf<Int>()

    private val registerTrackerFunction = pluginContext.referenceFunctions(
        callableId = CallableId(
            FqName("pro.mezentsev.tracker.internal.compose"),
            Name.identifier("registerTracking")
        )
    ).single()

    fun irBuildBody(
        function: IrFunction,
        body: IrBody
    ): IrBody {
        val statements = (body as? IrBlockBody)?.statements ?: return body

        return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
            statements.forEach { statement ->
                statement.accept(
                    StatementTransformer(
                        this,
                        messageCollector,
                        function.valueParameters
                    ),
                    null
                )
            }

            +statements
        }
    }

    private inner class StatementTransformer(
        private val irBlockBodyBuilder: IrBlockBodyBuilder,
        private val messageCollector: MessageCollector,
        private val valueParameters: List<IrValueParameter>,
    ) : IrElementTransformerVoidWithContext() {
        private var stateVariables = mutableListOf<IrVariable>()
        private var stateExpressions = mutableListOf<IrExpression>()

        override fun visitFunctionNew(declaration: IrFunction): IrStatement {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "TRACKER: inspect function: ${declaration.name.asString()}"
            )
            return super.visitFunctionNew(declaration)
        }

        override fun visitGetValue(expression: IrGetValue): IrExpression {
            if (expression.type.isStateVariable()) {
                messageCollector.report(
                    CompilerMessageSeverity.WARNING,
                    "TRACKER: found expression: ${expression.symbol.owner.name.asString()}"
                )
                stateExpressions.add(expression)
            }
            return super.visitGetValue(expression)
        }

        override fun visitVariable(declaration: IrVariable): IrStatement {
            // TODO remove "test"
            if (declaration.name.asString() == "test" && declaration.type.isStateVariable()) {
                messageCollector.report(
                    CompilerMessageSeverity.WARNING,
                    "TRACKER: found variable: ${declaration.dump()} ${declaration.type.classFqName} is? ${declaration.type.isStateVariable()}"
                )
                stateVariables.add(declaration)
            }

            return super.visitVariable(declaration)
        }

        override fun visitWhen(expression: IrWhen): IrExpression {
            expression.branches.forEach { branch ->
                val condition = branch.condition
                val result = branch.result

                when (condition) {
                    is IrCall -> messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "TRACKER: [when] found call: ${condition.symbol.owner.fqNameWhenAvailable?.asString()}"
                    )

                    is IrConst<*> -> messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "TRACKER: [when] found const: ${condition.value}"
                    )
                }

                when (result) {
                    is IrCall -> messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "TRACKER: [result] found call: ${result.symbol.owner.fqNameWhenAvailable?.asString()}"
                    )

                    is IrConst<*> -> messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "TRACKER: [result] found const: ${result.value}"
                    )

                    is IrBlock -> {
                        messageCollector.report(
                            CompilerMessageSeverity.WARNING,
                            "TRACKER: [result] found block: ${result.statements}"
                        )

                        val newStatements = mutableListOf<IrStatement>()
                        result.statements.forEach { statement ->
                            statement.transformStatement(
                                StatementTransformer(
                                    irBlockBodyBuilder,
                                    messageCollector,
                                    valueParameters
                                )
                            )

                            messageCollector.report(
                                CompilerMessageSeverity.WARNING,
                                "TRACKER: [statement] transformed ${statement}"
                            )

                            newStatements.add(statement)
                        }

                        stateVariables.firstOrNull()?.let {
                            if (it.hashCode() in setOfVariables) {
                                messageCollector.report(
                                    CompilerMessageSeverity.WARNING,
                                    "TRACKER: [variable] already added: ${it.name.asString()}"
                                )
                                return@let
                            }

                            messageCollector.report(
                                CompilerMessageSeverity.WARNING,
                                "TRACKER: [variable] add new statement: ${it.name.asString()}"
                            )

                            irBlockBodyBuilder.irTrack(it)?.let { call ->
                                // TODO: can't provide statement for $test variables
                                newStatements.add(call)

                                messageCollector.report(
                                    CompilerMessageSeverity.WARNING,
                                    "TRACKER: [dump]\n ${call.dump()}"
                                )
                                setOfVariables.add(it.hashCode())
                            }
                        }

                        stateExpressions.clear()
                        stateVariables.clear()

                        result.statements.clear()
                        result.statements.addAll(newStatements)
                    }
                }
            }

            return super.visitWhen(expression)
        }

        private fun IrType.isStateVariable(): Boolean {
            // Check if the type matches MutableState or similar pattern
            val fqName = classFqName?.asString() ?: return false

            return fqName.startsWith("androidx.compose.runtime.") &&
                (fqName.contains("MutableState") ||
                    fqName.contains("MutableIntState") ||
                    fqName.contains("MutableFloatState") ||
                    fqName.contains("MutableLongState") ||
                    fqName.contains("MutableDoubleState") ||
                    fqName.contains("MutableBooleanState"))
        }

        private fun IrBlockBodyBuilder.irTrack(
            stateVariable: IrVariable
        ): IrExpression? {
            val composer = valueParameters.firstOrNull { it.name.asString() == "\$composer" }
                ?: run {
                    messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "Not found composer in variables: ${valueParameters.map { it.name.asString() }}"
                    )
                    return null
                }

            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Registered tracker"
            )

            return irCall(registerTrackerFunction).also { call ->
                call.putValueArgument(0, irGet(stateVariable))
                call.putValueArgument(1, irGet(composer))
                call.putValueArgument(2, irString("test-1"))
                call.putValueArgument(3, irString("test-2"))
            }
        }
    }
}
