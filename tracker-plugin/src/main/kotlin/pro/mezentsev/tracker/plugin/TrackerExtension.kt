package pro.mezentsev.tracker.plugin

abstract class TrackerExtension {
    fun configureCompose(configure: ComposeExtension.() -> Unit) {
        compose.configure()
    }

    internal val compose: ComposeExtension = ComposeExtension()

    class ComposeExtension {
        var isEnabled: Boolean = true
        var includedClasses: List<String> = listOf()
        var excludedClasses: List<String> = listOf()
    }
}
