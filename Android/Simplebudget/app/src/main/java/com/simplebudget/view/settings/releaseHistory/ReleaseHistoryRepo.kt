package com.simplebudget.view.settings.releaseHistory

import java.util.*

/**
 */
class ReleaseHistoryRepo {
    val historyList: List<ReleaseHistory>
        get() {
            val releaseList = ArrayList<ReleaseHistory>()

            // 1.0.8
            releaseList.add(ReleaseHistory("Version", "1.0.8", "App password protection option added."))
            releaseList.add(ReleaseHistory("Version", "1.0.8", "Change start day of weeks functionality added."))
            releaseList.add(ReleaseHistory("Version", "1.0.8", "Recurring option for every 2 months added."))
            releaseList.add(ReleaseHistory("Version", "1.0.8", "Opening reports upon clicking weekly report notification."))
            releaseList.add(ReleaseHistory("Version", "1.0.8", "Settings click app version to see release notes."))
            releaseList.add(ReleaseHistory("Version", "1.0.8", "Performance and design enhancements"))

            // 1.0.7
            releaseList.add(ReleaseHistory("Version", "1.0.7", "Hide / Show balance feature added for privacy."))
            releaseList.add(ReleaseHistory("Version", "1.0.7", "Option added to start week from Sunday or Monday."))
            releaseList.add(ReleaseHistory("Version", "1.0.7", "Currency search functionality added."))
            releaseList.add(ReleaseHistory("Version", "1.0.7", "Performance and design enhancements"))


            return releaseList
        }
}
