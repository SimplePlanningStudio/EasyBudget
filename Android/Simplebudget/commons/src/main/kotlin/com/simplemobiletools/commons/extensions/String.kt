package com.simplemobiletools.commons.extensions

import com.bumptech.glide.signature.ObjectKey
import com.simplemobiletools.commons.helpers.*
import java.io.File
import java.text.Normalizer


fun String.getFileSignature(lastModified: Long? = null) = ObjectKey(getFileKey(lastModified))

fun String.getFileKey(lastModified: Long? = null): String {
    val file = File(this)
    val modified = if (lastModified != null && lastModified > 0) {
        lastModified
    } else {
        file.lastModified()
    }

    return "${file.absolutePath}$modified"
}


// remove diacritics, for example č -> c
fun String.normalizeString() =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(normalizeRegex, "")

// if we are comparing phone numbers, compare just the last 9 digits
fun String.trimToComparableNumber(): String {
    val normalizedNumber = this.normalizeString()
    val startIndex = Math.max(0, normalizedNumber.length - 9)
    return normalizedNumber.substring(startIndex)
}
