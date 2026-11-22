package com.example.news

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    // Add or remove any RSS feed here
    private val sources = linkedMapOf(
        "BBC News" to "https://feeds.bbci.co.uk/news/rss.xml",
        "The Guardian" to "https://www.theguardian.com/world/rss",
        "NPR" to "https://feeds.npr.org/1001/rss.xml",
        "Al Jazeera" to "https://www.aljazeera.com/xml/rss/all.xml"
    )
    private var links = listOf<String>()

    // TODO: replace with your real banner ad unit ID (looks like ca-app-pub-XXXX/YYYY).
    // This is Google's official TEST banner ID, safe to use while developing.
    private val bannerAdUnitId = "ca-app-pub-3940256099942544/6300978111"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}
        val banner = AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = bannerAdUnitId
            loadAd(AdRequest.Builder().build())
        }

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item, sources.keys.toList())
        }
        val list = ListView(this)
        val status = TextView(this).apply { setPadding(24, 8, 24, 8) }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(banner)
            addView(spinner)
            addView(status)
            addView(list)
        })

        list.setOnItemClickListener { _, _, pos, _ ->
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(links[pos])))
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                status.text = "Loading..."
                val url = sources.values.toList()[pos]
                thread {
                    try {
                        val news = fetch(url)
                        runOnUiThread {
                            links = news.map { it.second }
                            list.adapter = ArrayAdapter(this@MainActivity,
                                android.R.layout.simple_list_item_1, news.map { it.first })
                            status.text = "${news.size} stories"
                        }
                    } catch (e: Exception) {
                        runOnUiThread { status.text = "Failed to load: ${e.message}" }
                    }
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    // Minimal RSS parser: returns (title, link) pairs
    private fun fetch(url: String): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        URL(url).openStream().use { stream ->
            parser.setInput(stream, null)
            var inItem = false
            var title = ""
            var link = ""
            var type = parser.eventType
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> { inItem = true; title = ""; link = "" }
                        "title" -> if (inItem) title = parser.nextText().trim()
                        "link" -> if (inItem) link = parser.nextText().trim()
                    }
                } else if (type == XmlPullParser.END_TAG && parser.name == "item") {
                    inItem = false
                    if (title.isNotEmpty() && link.isNotEmpty()) out.add(title to link)
                }
                type = parser.next()
            }
        }
        return out
    }
}
