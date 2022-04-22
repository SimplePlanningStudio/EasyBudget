package com.simplebudget.helper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.Html
import android.widget.Toast

/**
 * Created by Waheed
 */
fun intentOpenWebsite(activity: Activity, url: String) {
    val openURL = Intent(Intent.ACTION_VIEW)
    openURL.data = Uri.parse(url)
    activity.startActivity(openURL)
}

fun intentShareCSV(activity: Activity, uri: Uri) {
    try {
        Toast.makeText(activity, "Sharing spreadsheet file....", Toast.LENGTH_SHORT).show()
        val shareIntent = Intent()
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.setDataAndType(uri, activity.contentResolver.getType(uri))
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.type = "text/file"
        activity.startActivity(Intent.createChooser(shareIntent, "Send to"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
