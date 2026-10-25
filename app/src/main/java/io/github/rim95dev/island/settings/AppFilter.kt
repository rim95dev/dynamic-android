package io.github.rim95dev.island.settings

enum class AppFilterMode {
    /** Every app is shown except [AppFilter.excludedPackages]. */
    ALL_APPS,

    /** No app is shown except [AppFilter.includedPackages]. */
    NO_APPS,
}

/**
 * Decides which apps' notifications appear in the island.
 *
 * Each mode keeps its own exception list, so switching modes never turns hidden apps into shown apps.
 */
data class AppFilter(
    val mode: AppFilterMode = AppFilterMode.ALL_APPS,
    val excludedPackages: Set<String> = emptySet(),
    val includedPackages: Set<String> = emptySet(),
) {
    val exceptions: Set<String>
        get() = when (mode) {
            AppFilterMode.ALL_APPS -> excludedPackages
            AppFilterMode.NO_APPS -> includedPackages
        }

    fun allows(packageName: String): Boolean = when (mode) {
        AppFilterMode.ALL_APPS -> packageName !in excludedPackages
        AppFilterMode.NO_APPS -> packageName in includedPackages
    }

    fun withException(packageName: String, isException: Boolean): AppFilter {
        val updated = if (isException) exceptions + packageName else exceptions - packageName
        return when (mode) {
            AppFilterMode.ALL_APPS -> copy(excludedPackages = updated)
            AppFilterMode.NO_APPS -> copy(includedPackages = updated)
        }
    }
}
