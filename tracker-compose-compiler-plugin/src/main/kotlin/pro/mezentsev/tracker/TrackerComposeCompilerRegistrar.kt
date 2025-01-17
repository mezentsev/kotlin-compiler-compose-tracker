@file:OptIn(ExperimentalCompilerApi::class)

package pro.mezentsev.tracker

import pro.mezentsev.tracker.TrackerComposeCommandLineProcessor.Companion.ENABLED
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

class TrackerComposeCompilerRegistrar : CompilerPluginRegistrar() {
    private lateinit var messageCollector: MessageCollector

    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        messageCollector = configuration.get(
            CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            MessageCollector.NONE
        )
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "Tracker Compose Compiler Plugin applied: ${configuration.get(ENABLED, true)}"
        )

        if (configuration.get(ENABLED, true)) {
            IrGenerationExtension.registerExtension(
                TrackerComposeIrGenerationExtension(messageCollector)
            )
        }
    }
}