package pro.mezentsev.tracker.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class TrackerComposeCompilerPlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project): Unit = with(target) {
        extensions.create(
            "tracker",
            TrackerExtension::class.java
        )
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String =
        "pro.mezentsev.tracker.tracker-compose-compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "pro.mezentsev.tracker",
        artifactId = "tracker-compose-compiler-plugin",
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(TrackerExtension::class.java)
        return project.provider {
            listOf(
                SubpluginOption(
                    key = "enabled",
                    lazyValue = lazy { extension.compose.isEnabled.toString() }
                ),
            )
        }
    }
}
