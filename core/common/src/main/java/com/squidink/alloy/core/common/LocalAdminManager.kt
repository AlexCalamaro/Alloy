package com.squidink.alloy.core.common

/**
 * Interface providing pluggable access to optional local Device Admin / Device Owner capabilities.
 */
interface LocalAdminManager {

    /**
     * Indicates whether the application is currently granted Device Owner / Device Admin privileges.
     */
    fun isDeviceAdminGranted(): Boolean

    /**
     * Executes an optional admin action if privileges exist.
     */
    fun executeAdminAction(actionId: String): Boolean
}

/**
 * Default No-Op implementation for consumer builds.
 */
class NoOpLocalAdminManager : LocalAdminManager {
    override fun isDeviceAdminGranted(): Boolean = false
    override fun executeAdminAction(actionId: String): Boolean = false
}
