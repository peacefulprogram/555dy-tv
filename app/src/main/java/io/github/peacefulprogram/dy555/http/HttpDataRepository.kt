package io.github.peacefulprogram.dy555.http

import io.github.peacefulprogram.dy555.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.math.min

class HttpDataRepository(private val okHttpClient: OkHttpClient) {

    private fun getDocument(url: String): Document {
        val html = Request.Builder()
            .url(url)
            .get()
            .build()
            .let {
                okHttpClient.newCall(it).execute()
            }
            .body!!
            .string()

        return Jsoup.parse(html)
    }

    private fun getIdFromUrl(url: String): String =
        url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'))

    fun getHomePage(): List<Pair<String, List<MediaCardData>>> {
        val document = getDocument(Constants.BASE_URL)
        val swiperPanels =
            document.selectFirst(".main .content .sm-swiper .swiper-wrapper")?.children()!!
        val wideRecommends = swiperPanels.map { panel ->
            val title = panel.selectFirst(".title")!!.text().trim()
            val pic = panel.selectFirst("img")!!.attr("src")
            val id = panel.selectFirst("a")!!.attr("href").let {
                it.substring(it.lastIndexOf('/') + 1, it.lastIndexOf("."))
            }
            val note = panel.selectFirst(".ins p")!!.text().trim()
            MediaCardData(
                id = id,
                title = title,
                pic = pic,
                note = note
            )
        }
        val resultList = mutableListOf<Pair<String, List<MediaCardData>>>()
        resultList.add(Pair("推荐", wideRecommends))
        val otherParts =
            setOf(
                "本周最佳电影",
                "Netflix奈飞蓝光4K剧",
                "每周热门日韩剧排行",
                "每周热门欧美剧排行",
                "每周热门港台剧排行",
                "每周热门连续剧排行",
                "每周热门动漫排行",
                "每周热门综艺纪录排行"
            )
        document.select(".module").forEach { moduleEl ->
            val title =
                moduleEl.selectFirst(".module-title")
                    ?.text()
                    ?.trim()
                    ?.takeIf(otherParts::contains)
                    ?: return@forEach
            val groupVideos = moduleEl.select(".module-main .module-item").map { videoItem ->
                val href = videoItem.attr("href")
                val videoTitle = videoItem.attr("title")
                val pic = videoItem.selectFirst("img")!!.dataset()["original"]!!
                val note = videoItem.selectFirst(".module-item-note")?.text()?.trim()
                MediaCardData(
                    id = getIdFromUrl(href),
                    title = videoTitle,
                    pic = if (pic.startsWith("http")) pic else "https://www.555dy.cc$pic",
                    note = note
                )
            }
            resultList.add(Pair(title, groupVideos))
        }
        return resultList
    }


    fun getDetailPage(videoId: String): VideoDetailData {
        val document = getDocument("${Constants.BASE_URL}/voddetail/$videoId.html")
        val infoContainer = document.selectFirst(".main .module-main")!!
        val cover = infoContainer.selectFirst("img")!!.dataset()["original"]!!
        val title = infoContainer.selectFirst(".module-info-heading h1")!!.text().trim()
        val tags = infoContainer.select(".module-info-tag-link a").map { link ->
            VideoTag(
                name = link.text().trim(),
                url = link.attr("href")
            )
        }
        val desc = infoContainer.selectFirst(".module-info-introduction-content")!!.text()
        val infoLines = infoContainer.select(".module-info-item").asSequence()
            .filter { !it.hasClass("module-info-introduction") }
            .map { div ->
                val lineName = div.child(0).text().trim()
                if (lineName.contains("豆瓣") || lineName.contains("编剧")) {
                    return@map VideoInfoLine.PlainTextInfo(
                        name = lineName,
                        value = div.child(1).text().trim().trim('/').replace("/", " / ")
                    )
                }
                val links = div.child(1).select("a")
                if (links.isEmpty()) {
                    VideoInfoLine.PlainTextInfo(
                        name = lineName,
                        div.child(1).text().trim().trim('/').replace("/", " / ")
                    )
                } else {
                    VideoInfoLine.TagInfo(
                        name = lineName,
                        tags = links
                            .map { l ->
                                VideoTag(
                                    name = l.text().trim(),
                                    url = l.attr("href")
                                )
                            }
                            .filter { it.name.isNotEmpty() }
                    )
                }
            }
            .toList()
        val tabItems = document.select("#y-playList .tab-item")
        val playListEls = document.select(".module-play-list-content")
        val playLists = mutableListOf<Pair<String, List<Episode>>>()
        for (i in 0 until min(tabItems.size, playListEls.size)) {
            val episodes = playListEls[i].children()
                .map { a ->
                    val href = a.attr("href")
                    Episode(
                        id = href.substring(href.lastIndexOf('/') + 1, href.lastIndexOf('.')),
                        name = a.text().trim()
                    )
                }
            playLists.add(Pair(tabItems[i].child(0).text(), episodes))
        }
        val relatedVideos = document.selectFirst(".module-poster-items-base")
            ?.children()
            ?.map(this::parseVideoLinkElement) ?: emptyList()
        return VideoDetailData(
            id = videoId,
            title = title,
            desc = desc,
            pic = if (cover.startsWith("http")) cover else "${Constants.BASE_URL}$cover",
            playLists = playLists,
            relatedVideos = relatedVideos,
            tags = tags,
            infoLines = infoLines
        )
    }

    fun getNetflix(page: Int): PageResult<MediaCardData> {
        val document = getDocument("${Constants.BASE_URL}/label/netflix/page/${page}.html")
        val videoList = document.select(".module-items > a").map(this::parseVideoLinkElement)
        return PageResult(
            data = videoList,
            page = page,
            hasNextPage = hasNextPage(document)
        )
    }

    fun getVideoPageByType(typeId: Int): List<Pair<String, List<MediaCardData>>> {
        val document = getDocument("${Constants.BASE_URL}/vodtype/${typeId}.html")
        val modules = document.getElementsByClass("module")
        val groups = mutableListOf<Pair<String, List<MediaCardData>>>()
        for (module in modules) {
            val moduleTitle =
                module.getElementsByClass("module-title").firstOrNull()?.text() ?: "推荐"
            val videos = module.select(".module-items > a").map(this::parseVideoLinkElement)
            groups.add(Pair(moduleTitle, videos))
        }
        return groups
    }

    private fun parseVideoLinkElement(element: Element): MediaCardData {
        val id = getIdFromUrl(element.attr("href"))
        val pic = element.selectFirst("img")!!.dataset()["original"]!!
        val title = element.getElementsByClass("module-poster-item-title")[0]!!.text().trim()
        val note = element.getElementsByClass("module-item-note").firstOrNull()?.text()?.trim()
        return MediaCardData(
            id = id,
            title = title,
            pic = pic,
            note = note
        )
    }

    private fun hasNextPage(document: Document): Boolean {
        val pageContainer = document.getElementById("page")!!
        val currentPage =
            pageContainer.getElementsByClass("page-current").firstOrNull()?.text()?.trim()
                ?: return false
        val lastPageHref =
            pageContainer.child(pageContainer.childrenSize() - 1).attr("href") ?: return false
        return !lastPageHref.endsWith("/$currentPage.html")
    }
}