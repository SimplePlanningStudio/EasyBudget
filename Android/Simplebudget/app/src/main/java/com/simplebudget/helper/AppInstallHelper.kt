package com.simplebudget.helper

import android.content.Context

object AppInstallHelper {

    fun isInstalled(packageName: String, context: Context): Boolean {
        val packageManager = context.packageManager
        val packages = packageManager.getInstalledPackages(0)
        return packages.any { it.packageName == packageName }
    }
}