package com.simplebudget.view.report

import java.io.File

data class ExportStatus(
    val status: Boolean = false,
    val message: String = "",
    val path: String = "",
    val file: File? = null
)