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
package com.simplebudget.helper

import java.util.*

object DelayHelper {

    /**
     * Calculate initial delay and set it to 6pm = 18, 7pm = 19
     * Default 18
     */
    fun calculateInitialDelay(timeHour: Int = 18): Long {
        val currentTime = System.currentTimeMillis()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        // Set the desired time e.g 18 = 6PM, 19 = 7PM etc.
        calendar.set(Calendar.HOUR_OF_DAY, timeHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val desiredTime = calendar.timeInMillis
        // If the desired time has already passed for today, set it for tomorrow
        if (desiredTime <= currentTime) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis - currentTime
    }


    fun calculateInitialDelayForTesting(): Long {
        val currentTime = System.currentTimeMillis()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        // Calculate the next 5-minute interval
        val currentMinute = calendar.get(Calendar.MINUTE)
        val next5Minute = (currentMinute / 2 + 1) * 2
        calendar.set(Calendar.MINUTE, next5Minute)
        calendar.set(Calendar.SECOND, 0)

        return calendar.timeInMillis - currentTime
    }
}