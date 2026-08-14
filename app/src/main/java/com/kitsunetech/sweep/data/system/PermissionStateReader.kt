package com.kitsunetech.sweep.data.system

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Process

data class PermissionState(
    val allFilesAccess: Boolean,
    val usageAccess: Boolean,
    val allFilesAccessAvailable: Boolean = true,
)

fun interface PermissionStateSource {
    fun read(): PermissionState
}

class PermissionStateReader(
    context: Context,
) : PermissionStateSource {
    private val applicationContext = context.applicationContext
    private val appOpsManager = applicationContext.getSystemService(AppOpsManager::class.java)

    override fun read(): PermissionState = PermissionState(
        allFilesAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager(),
        usageAccess = usageMode() == AppOpsManager.MODE_ALLOWED,
        allFilesAccessAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
    )

    @SuppressLint("WrongConstant")
    @Suppress("DEPRECATION")
    private fun usageMode(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            applicationContext.packageName,
        )
    } else {
        appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            applicationContext.packageName,
        )
    }
}
