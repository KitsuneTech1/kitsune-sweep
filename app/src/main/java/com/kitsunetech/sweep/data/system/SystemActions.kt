package com.kitsunetech.sweep.data.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.storage.StorageManager
import android.provider.Settings

data class IntentDescriptor(
    val action: String,
    val dataUri: String?,
)

sealed interface SystemActionSpec {
    data object RequestAllFiles : SystemActionSpec
    data object RequestUsage : SystemActionSpec
    data object ManageStorage : SystemActionSpec
    data object ClearExternalCaches : SystemActionSpec
    data class AppDetails(val packageName: String) : SystemActionSpec
    data class Uninstall(val packageName: String) : SystemActionSpec
}

fun SystemActionSpec.toDescriptor(appPackageName: String): IntentDescriptor {
    val ownPackage = validatePackageName(appPackageName)
    return when (this) {
        SystemActionSpec.RequestAllFiles -> IntentDescriptor(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            "package:$ownPackage",
        )
        SystemActionSpec.RequestUsage -> IntentDescriptor(
            Settings.ACTION_USAGE_ACCESS_SETTINGS,
            "package:$ownPackage",
        )
        SystemActionSpec.ManageStorage -> IntentDescriptor(
            Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            null,
        )
        SystemActionSpec.ClearExternalCaches -> IntentDescriptor(
            StorageManager.ACTION_CLEAR_APP_CACHE,
            null,
        )
        is SystemActionSpec.AppDetails -> IntentDescriptor(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${validatePackageName(packageName)}",
        )
        is SystemActionSpec.Uninstall -> IntentDescriptor(
            Intent.ACTION_DELETE,
            "package:${validatePackageName(packageName)}",
        )
    }
}

fun SystemActionSpec.toIntent(appPackageName: String): Intent =
    toDescriptor(appPackageName).toIntent()

fun SystemActionSpec.resolveIntent(context: Context): Intent? =
    toIntentCandidates(context.packageName).firstOrNull { candidate ->
        candidate.resolveActivity(context.packageManager) != null
    }

private fun SystemActionSpec.toIntentCandidates(appPackageName: String): List<Intent> {
    val primary = toIntent(appPackageName)
    if (this != SystemActionSpec.ManageStorage) return listOf(primary)
    return listOf(
        primary,
        Intent(StorageManager.ACTION_MANAGE_STORAGE),
        Intent(Settings.ACTION_SETTINGS),
    )
}

private fun IntentDescriptor.toIntent(): Intent = Intent(action).apply {
    dataUri?.let { data = Uri.parse(it) }
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun validatePackageName(packageName: String): String {
    require(PACKAGE_NAME.matches(packageName)) { "Invalid Android package name" }
    return packageName
}

private val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
