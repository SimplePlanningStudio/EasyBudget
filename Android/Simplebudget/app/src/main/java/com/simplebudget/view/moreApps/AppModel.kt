/*
 *   Copyright 2023 Waheed Nazir
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
package com.simplebudget.view.moreApps

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

class AppModel {
    @SerializedName("data")
    @Expose
    var data: List<AppItem> = ArrayList()

    inner class AppItem {
        @SerializedName("title")
        @Expose
        var title: String? = ""

        @SerializedName("message")
        @Expose
        var message: String? = ""

        @SerializedName("imagelink")
        @Expose
        var imagelink: String? = ""

        @SerializedName("store_link")
        @Expose
        var storeLink: String? = ""
    }
}