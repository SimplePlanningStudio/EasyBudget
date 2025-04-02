/*
 *   Copyright 2025 Waheed Nazir
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.simplebudget.helper.analytics

import com.google.firebase.analytics.FirebaseAnalytics.Event

object Events {
    //General
    const val KEY_TELEGRAM = "telegram"
    const val KEY_WHATSAPP = "whatsapp"
    const val KEY_YOUTUBE = "youtube"
    const val KEY_FEEDBACK_GMAIL = "feedback_gmail"
    const val KEY_SCREEN_NAME = "screen_name"
    const val KEY_BUTTON_CLICK = "button_click"
    const val KEY_REVENUE = "revenue"
    const val KEY_EXPENSE = "expense"
    const val KEY_RESULT = "result"
    const val KEY_VALUE = "value"
    const val KEY_STATUS = "status"
    const val KEY_PERMISSION_GRANTED = "permission_granted"
    const val KEY_PERMISSION_NOT_GRANTED = "permission_not_granted"

    //Currency
    const val KEY_CURRENCY_SELECTED = "currency_selected"

    //Balance
    const val KEY_ADDED_BALANCE = "added_balance"

    //Notification
    const val KEY_NOTIFICATION_PERMISSION = "notification_permission"
    const val KEY_NOTIFICATION_RECEIVED = "notification_received"

    //Search
    const val KEY_SEARCH_FILTER = "search_filter"
    const val KEY_SEARCH_EXPORT = "search_export"
    const val KEY_SEARCH_RESET = "reset_to_default"

    //Report
    const val KEY_REPORT_EXPORT = "report_export"
    const val KEY_PRINT_PDF_REPORT = "print_pdf_report"
    const val KEY_PRINT_EXCEL_REPORT = "print_excel_report"

    //Account

    //Backup
    const val KEY_LOGIN_SUCCESS = "login_success"
    const val KEY_LOGIN_ERROR = "login_error"
    const val KEY_LOG_OUT = "logout"
    const val KEY_BACKUP_ENABLED = "backup_enabled"
    const val KEY_BACKUP_DISABLED = "backup_disabled"
    const val KEY_BACKUP_SUCCESS = "backup_success"
    const val KEY_BACKUP_ERROR = "backup_error"
    const val KEY_BACKUP_DELETED = "backup_deleted"
    const val KEY_BACKUP_DELETE_ERROR = "backup_delete_error"
    const val KEY_BACKUP_RESTORED = "backup_restored"
    const val KEY_BACKUP_RESTORE_DATA_ERROR = "restored_data_error"
    const val KEY_BACKUP_RESTORE_DATA_IGNORE = "restored_data_ignore"

    //FAQ
    const val KEY_FAQ = "faq"
    const val KEY_FAQ_VOICE_SEARCHED = "faq_voiced_searched"

    //Side menu
    const val KEY_SIDE_MENU_ACCOUNTS = "side_menu_accounts"
    const val KEY_SIDE_MENU_BUDGETS = "side_menu_budgets"
    const val KEY_SIDE_MENU_SETTINGS = "side_menu_settings"
    const val KEY_SIDE_MENU_LINEAR_BREAKDOWN = "side_menu_linear_breakdown"
    const val KEY_SIDE_MENU_PI_CHART = "side_menu_pie_chart"
    const val KEY_SIDE_MENU_ADJUST_BALANCE = "side_menu_adjust_balance"
    const val KEY_SIDE_MENU_CATEGORIES = "side_menu_categories"
    const val KEY_SIDE_MENU_SHARE_APP = "side_menu_share_app"
    const val KEY_SIDE_MENU_RATE_APP = "side_menu_rate_app"
    const val KEY_SIDE_MENU_WHAT_PEOPLE_SAY = "side_menu_what_people_say"

    //Account
    const val KEY_ACCOUNT_DETAILS = "account_details"
    const val KEY_ACCOUNTS_COUNTS = "accounts_counts"
    const val KEY_ADD_ACCOUNT_CLICKED = "add_account_clicked"
    const val KEY_ACCOUNT_ADDED = "account_added"
    const val KEY_ACCOUNT_SWITCHED = "account_switched"
    const val KEY_ACCOUNT_UPDATED = "account_updated"

    //Budget
    const val KEY_BUDGETS_FROM_INTRO = "budgets_from_intro"
    const val KEY_BUDGETS = "budgets"
    const val KEY_BUDGETS_MONTHS = "budgets_months" // Month counts
    const val KEY_BUDGETS_COUNT = "budgets_count"
    const val KEY_ADD_BUDGET = "add_budget"
    const val KEY_EDIT_BUDGET = "edit_budget"
    const val KEY_BUDGET_ADDED = "budget_added"
    const val KEY_ADD_BUDGET_INTERVAL = "add_budget_interval" // Monthly or one time etc.
    const val KEY_ADD_BUDGET_CATEGORIES = "dd_budget_categories"

    //Break Down
    const val KEY_LINEAR_BREAKDOWN_FILTER = "breakdown_filter" // All, revenues or expenses
    const val KEY_LINEAR_BREAKDOWN = "linear_breakdown"
    const val KEY_PIE_BREAKDOWN = "pie_breakdown"
    const val KEY_PIE_BREAKDOWN_EXPENSES_COUNT = "pie_breakdown_expenses_count"

    //Rate App
    const val KEY_RATE_APP_LIKE = "rate_app_liked"
    const val KEY_RATE_APP_DISLIKE = "rate_app_disliked"
    const val KEY_REVIEW_APP_RATE = "review_app_rate"
    const val KEY_REVIEW_DON_NOT_ASK_AGAIN = "rate_do_not_ask_again"
    const val KEY_REVIEW_FEEDBACK_NO_THANKS = "rate_feedback_no_thanks"
    const val KEY_REVIEW_FEEDBACK = "rate_feedback"

    //Dashboard
    const val KEY_DASHBOARD_ABOUT_US_LOGO = "dashboard_about_us_logo"
    const val KEY_DASHBOARD_ABOUT_US_DRAWER = "dashboard_about_us_drawer"
    const val KEY_DASHBOARD_SEARCH_BUTTON = "dashboard_search_button"
    const val KEY_DASHBOARD_REPORT_BUTTON = "dashboard_report_button"
    const val KEY_DASHBOARD_BACKUP_BUTTON = "dashboard_backup_button"
    const val KEY_DASHBOARD_HELP_BUTTON = "dashboard_help_button"
    const val KEY_DASHBOARD_SETTINGS_BUTTON = "dashboard_settings_button"
    const val KEY_DASHBOARD_CALENDAR_BUTTON = "dashboard_calendar_button"
    const val KEY_DASHBOARD_CALENDAR_OPEN = "calendar_opened"
    const val KEY_DASHBOARD_CALENDAR_CLOSED = "calendar_closed"
    const val KEY_DASHBOARD_PREMIUM_BUTTON = "dashboard_premium_button_on_balance"
    const val KEY_DASHBOARD_BALANCE_PRIVACY = "balance_privacy"
    const val KEY_DASHBOARD_BALANCE_PRIVACY_ON = "dashboard_balance_privacy_on"
    const val KEY_DASHBOARD_BALANCE_PRIVACY_OFF = "dashboard_balance_privacy_off"

    //Categories
    const val KEY_CATEGORY_ADDED = "category_added"
    const val KEY_CATEGORY_DELETED = "category_deleted"
    const val KEY_CATEGORY_EDITED = "category_edited"
    const val KEY_CATEGORY_SELECTED = "category_selected"
    const val KEY_CATEGORY_SORTED = "category_sorted"
    const val KEY_CATEGORY_VOICE_SEARCHED = "category_voice_searched"
    const val KEY_CATEGORIES_COUNT = "categories_count"

    //Settings
    const val KEY_SETTINGS_CHANGE_CURRENCY = "settings_change_currency"
    const val KEY_SETTINGS_LOW_BALANCE_THRESHOLD = "settings_low_balance_threshold"
    const val KEY_SETTINGS_CHANGE_START_DAY_OF_WEEK = "settings_change_start_day_of_week"
    const val KEY_SETTINGS_APP_PASSWORD_PROTECTION = "settings_app_password_protection"
    const val KEY_SETTINGS_MONTHLY_REPORT = "settings_monthly_report"
    const val KEY_SETTINGS_BREAKDOWN = "settings_expenses_breakdown"
    const val KEY_SETTINGS_PIE_CHART = "settings_pie_chart"
    const val KEY_SETTINGS_DATA_BACKUP = "settings_data_backup"
    const val KEY_SETTINGS_REMOVE_ADS = "settings_remove_ads"
    const val KEY_SETTINGS_PROMO_CODE = "settings_promo_code"
    const val KEY_SETTINGS_CONTACT_US = "settings_contact_us"
    const val KEY_SETTINGS_RATE_APP = "settings_rate_app"
    const val KEY_SETTINGS_SHARE_APP = "settings_share_app"
    const val KEY_SETTINGS_ABOUT_US = "settings_about_us"
    const val KEY_SETTINGS_RESET_APP_DATA = "settings_reset_app_data"
    const val KEY_SETTINGS_RESET_APP_DONE = "settings_reset_app_done"

    //Premium
    const val KEY_PREMIUM_SUBSCRIPTION_CLICKED = "premium_subscription_clicked"
    const val KEY_PREMIUM_LIFE_TIME_CLICKED = "premium_lifetime_clicked"
    const val KEY_PREMIUM = "premium"
    const val KEY_NOT_PREMIUM = "not_premium"
    const val KEY_ACCOUNT_TYPE = "account_type"
    const val KEY_PREMIUM_PURCHASE_ERROR = "purchase_error"
    const val KEY_PREMIUM_PURCHASE_CANCELLED = "purchase_cancelled"
    const val KEY_PREMIUM_PURCHASE_SUCCESS = "purchase_success"

    //Add Edit Expense
    const val KEY_ADD_EXPENSE = "add_expense"
    const val KEY_ADD_RECURRING_EXPENSE = "add_recurring_expense"
    const val KEY_ADD_EXPENSE_SELECTED_INTERVAL = "selected_interval"
    const val KEY_ADD_EXPENSE_SELECTED_CATEGORY = "selected_category"

    //Promotion Banner
    const val KEY_PROMOTION_BANNER_SHOWN = "promotion_banner_shown"
    const val KEY_PROMOTION_BANNER_CLICKED = "promotion_banner_clicked"
    const val KEY_PROMOTION_BANNER_NAME = "promotion_banner_name"
    const val KEY_PROMOTION_BANNER_PACKAGE = "promotion_banner_package"

    //More Apps
    const val KEY_MORE_APPS = "more_apps"
    const val KEY_MORE_APPS_APP_CLICKED = "app_clicked"

    //Screen names
    const val KEY_DASHBOARD_SCREEN = "dashboard_screen"
    const val KEY_SEARCH_SCREEN = "search_screen"
    const val KEY_MONTHLY_REPORTS_SCREEN = "monthly_reports_screen"
    const val KEY_BACKUP_SCREEN = "backup_screen"
    const val KEY_CONTACT_US_SCREEN = "contact_us_screen"
    const val KEY_SETTINGS_SCREEN = "settings_screen"
    const val KEY_ACCOUNT_DETAILS_SCREEN = "account_details_screen"
    const val KEY_BUDGETS_SCREEN = "budgets_screen"
    const val KEY_LINEAR_BREAKDOWN_SCREEN = "linear_breakdown_screen"
    const val KEY_PIE_CHART_SCREEN = "pie_chart_screen"
    const val KEY_MANAGE_CATEGORIES_SCREEN = "manage_categories_screen"
    const val KEY_CHOOSE_CATEGORY_SCREEN = "choose_category_screen"
    const val KEY_ADD_EXPENSE_SCREEN = "add_expense_screen"
    const val KEY_ADD_RECURRING_SCREEN = "add_recurring_expense_screen"
    const val KEY_PASSWORD_PROTECTION_SCREEN = "password_protection_screen"
    const val KEY_REMOVE_ADS_SCREEN = "remove_ads_screen"
    const val KEY_FAQ_SCREEN = "faq_screen"
    const val KEY_ABOUT_US_SCREEN = "about_us_screen"
    const val KEY_MORE_APPS_SCREEN = "more_apps_screen"
    const val KEY_APP_RELEASE_SCREEN = "app_releases_screen"
    const val KEY_RESET_APP_SCREEN = "reset_app_screen"
    const val KEY_ADD_BUDGET_SCREEN = "add_budget_screen"
    const val KEY_BUDGET_DETAILS_SCREEN = "budget_details_screen"
    const val KEY_OPEN_SOURCE_SCREEN = "open_source_screen"
    const val KEY_USER_TESTIMONIAL_SCREEN = "user_testimonials_screen"
}