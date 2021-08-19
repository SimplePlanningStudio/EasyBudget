package com.simplebudget.view.settings.releaseHistory

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplebudget.R
import com.simplebudget.helper.stickytimelineview.callback.SectionCallback
import com.simplebudget.helper.stickytimelineview.model.SectionInfo
import kotlinx.android.synthetic.main.activity_release_history_timeline.*

class ReleaseHistoryTimelineActivity : AppCompatActivity() {
    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_release_history_timeline)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initVerticalRecyclerView()
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     *
     */
    private fun initVerticalRecyclerView() {
        val singerList = getSingerList()
        vertical_recycler_view.adapter = ReleaseHistoryAdapter(
            layoutInflater,
            singerList,
            R.layout.recycler_release_history_row
        )

        //Currently only LinearLayoutManager is supported.
        vertical_recycler_view.layoutManager = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )

        vertical_recycler_view.addItemDecoration(getSectionCallback(singerList))
    }

    //Get data method
    private fun getSingerList(): List<ReleaseHistory> = ReleaseHistoryRepo().historyList


    //Get SectionCallback method
    private fun getSectionCallback(singerList: List<ReleaseHistory>): SectionCallback {
        return object : SectionCallback {
            //In your data, implement a method to determine if this is a section.
            override fun isSection(position: Int): Boolean =
                singerList[position].versionCode != singerList[position - 1].versionCode

            //Implement a method that returns a SectionHeader.
            override fun getSectionHeader(position: Int): SectionInfo {
                val singer = singerList[position]
                return SectionInfo(singer.versionCode, singer.versionLabel)
            }

        }
    }

    /**
     *
     */
    private fun getSectionCallbackWithDrawable(singerList: List<ReleaseHistory>): SectionCallback {
        return object : SectionCallback {
            //In your data, implement a method to determine if this is a section.
            override fun isSection(position: Int): Boolean =
                singerList[position].versionCode != singerList[position - 1].versionCode

            //Implement a method that returns a SectionHeader.
            override fun getSectionHeader(position: Int): SectionInfo {
                val singer = singerList[position]
                return SectionInfo(singer.versionCode, singer.versionLabel)
            }

        }
    }
}
