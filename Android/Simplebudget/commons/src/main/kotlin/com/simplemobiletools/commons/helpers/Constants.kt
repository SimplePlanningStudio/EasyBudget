package com.simplemobiletools.commons.helpers

import android.graphics.Color
import android.os.Build
import android.os.Looper
import android.util.Log
import java.util.*

const val IS_CUSTOMIZING_COLORS = "is_customizing_colors"
val DEFAULT_WIDGET_BG_COLOR = Color.parseColor("#AA000000")
const val MD5 = "MD5"
const val SHORT_ANIMATION_DURATION = 150L
const val NAVIGATION_BAR_COLOR = "navigation_bar_color"
const val CUSTOM_PRIMARY_COLOR = "custom_primary_color"
const val CUSTOM_APP_ICON_COLOR = "custom_app_icon_color"
const val PROTECTION_TYPE = "protection_type"
const val APP_PASSWORD_PROTECTION = "app_password_protection"
const val APP_PASSWORD_HASH = "app_password_hash"
const val APP_PROTECTION_TYPE = "app_protection_type"
const val DELETE_PASSWORD_HASH = "delete_password_hash"
const val USE_24_HOUR_FORMAT = "use_24_hour_format"
const val VIBRATE_ON_BUTTON_PRESS = "vibrate_on_button_press"
const val YOUR_ALARM_SOUNDS = "your_alarm_sounds"
const val INITIAL_WIDGET_HEIGHT = "initial_widget_height"

const val LICENSE_ROBOLECTRIC = 32768
const val LICENSE_NUMBER_PICKER = 524288
const val LICENSE_PANORAMA_VIEW = 2097152
const val LICENSE_GESTURE_VIEWS = 8388608
const val LICENSE_EVENT_BUS = 33554432

// global intents
const val OPEN_DOCUMENT_TREE = 1000

// sorting
const val SORT_FOLDER_PREFIX = "sort_folder_"       // storing folder specific values at using "Use for this folder only"

// security
const val WAS_PROTECTION_HANDLED = "was_protection_handled"
const val PROTECTION_PIN = 0

const val SHOW_PIN = 0

const val PERMISSION_WRITE_CONTACTS = 6
const val PERMISSION_GET_ACCOUNTS = 12
const val PERMISSION_READ_SMS = 13
const val PERMISSION_SEND_SMS = 14

// conflict resolving
const val CONFLICT_SKIP = 1
const val CONFLICT_KEEP_BOTH = 4

const val MONDAY_BIT = 1
const val TUESDAY_BIT = 2
const val WEDNESDAY_BIT = 4
const val THURSDAY_BIT = 8
const val FRIDAY_BIT = 16
const val SATURDAY_BIT = 32
const val SUNDAY_BIT = 64
const val EVERY_DAY_BIT = MONDAY_BIT or TUESDAY_BIT or WEDNESDAY_BIT or THURSDAY_BIT or FRIDAY_BIT or SATURDAY_BIT or SUNDAY_BIT

const val TAB_RECENT_FILES = 32

const val DATE_FORMAT_EIGHT = "dd-MM-yyyy"


// view types
const val VIEW_TYPE_GRID = 1

fun isOnMainThread() = Looper.myLooper() == Looper.getMainLooper()

val normalizeRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()

val proPackages = arrayListOf("draw", "gallery", "filemanager", "contacts", "notes", "calendar")
