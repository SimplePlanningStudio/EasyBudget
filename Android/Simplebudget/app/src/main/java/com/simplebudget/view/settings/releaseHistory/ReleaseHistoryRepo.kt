package com.simplebudget.view.settings.releaseHistory

import java.util.*

/**
 */
class ReleaseHistoryRepo {
    val historyList: List<ReleaseHistory>
        get() {
            val releaseList = ArrayList<ReleaseHistory>()

            // 2.0.5
            releaseList.add(ReleaseHistory("Version", "2.0.5", "Create/Edit own categories for expenses."))
            releaseList.add(ReleaseHistory("Version", "2.0.5", "Telegram channel added for quick support and news."))
            releaseList.add(ReleaseHistory("Version", "2.0.5", "More apps section added."))
            releaseList.add(ReleaseHistory("Version", "2.0.5", "Privacy policy section added."))
            releaseList.add(ReleaseHistory("Version", "2.0.5", "UI enhancement and bug fixing."))

            // 2.0.2
            releaseList.add(ReleaseHistory("Version", "2.0.2", "Monthly reports now showing categories and total spending."))
            releaseList.add(ReleaseHistory("Version", "2.0.2", "Reports PDF design updated added categories."))
            releaseList.add(ReleaseHistory("Version", "2.0.2", "Feedback to email issue fixed."))
            releaseList.add(ReleaseHistory("Version", "2.0.2", "UI enhancement and bug fixing."))

            // 2.0.1
            releaseList.add(ReleaseHistory("Version", "2.0.1", "Monthly reports now showing categories and total spending."))
            releaseList.add(ReleaseHistory("Version", "2.0.1", "Reports PDF design updated added categories."))
            releaseList.add(ReleaseHistory("Version", "2.0.1", "Feedback to email issue fixed."))
            releaseList.add(ReleaseHistory("Version", "2.0.1", "UI enhancement and bug fixing."))

            // 2.0.0
            releaseList.add(ReleaseHistory("Version", "2.0.0", "Categories added for Incomes / Expenses."))
            releaseList.add(ReleaseHistory("Version", "2.0.0", "Categories added for monthly reports."))
            releaseList.add(ReleaseHistory("Version", "2.0.0", "Reports PDF design updated."))
            releaseList.add(ReleaseHistory("Version", "2.0.0", "Rating / In app review added for quick reviews."))
            releaseList.add(ReleaseHistory("Version", "2.0.0", "Feedback option added into settings."))
            releaseList.add(ReleaseHistory("Version", "2.0.0", "UI enhancement and bug fixing."))

            // 1.2.0
            releaseList.add(ReleaseHistory("Version", "1.2.0", "Premium acknowledgement screen added."))
            releaseList.add(ReleaseHistory("Version", "1.2.0", "Export monthly reports fixed few issues."))
            releaseList.add(ReleaseHistory("Version", "1.2.0", "UI enhancement and bug fixing."))

            // 1.1.0
            releaseList.add(ReleaseHistory("Version", "1.1.0", "Share monthly reports as spreadsheet."))
            releaseList.add(ReleaseHistory("Version", "1.1.0", "Print/Download monthly reports as pdf."))
            releaseList.add(ReleaseHistory("Version", "1.1.0", "Redeem promo code to buy premium."))
            releaseList.add(ReleaseHistory("Version", "1.1.0", "UI enhancement and bug fixing."))

            // 1.0.9
            releaseList.add(ReleaseHistory("Version", "1.0.9", "Expense displayed on dashboard."))
            releaseList.add(ReleaseHistory("Version", "1.0.9", "App security user interface redesigned."))

            // 1.0.8
            releaseList.add(ReleaseHistory("Version", "1.0.8", "App password protection option added."))
            releaseList.add(ReleaseHistory("Version", "1.0.8", "Change start day of weeks functionality added."))
            releaseList.add(ReleaseHistory("Version", "1.0.8", "Recurring option for every 2 months added."))
            releaseList.add(ReleaseHistory("Version", "1.0.8", "Opening reports upon clicking weekly report notification."))
            releaseList.add(ReleaseHistory("Version", "1.0.8", "Settings click app version to see release notes."))
            releaseList.add(ReleaseHistory("Version", "1.0.8", "Performance and design enhancements."))

            // 1.0.7
            releaseList.add(ReleaseHistory("Version", "1.0.7", "Hide / Show balance feature added for privacy."))
            releaseList.add(ReleaseHistory("Version", "1.0.7", "Option added to start week from Sunday or Monday."))
            releaseList.add(ReleaseHistory("Version", "1.0.7", "Currency search functionality added."))
            releaseList.add(ReleaseHistory("Version", "1.0.7", "Performance and design enhancements"))

            return releaseList
        }
}
