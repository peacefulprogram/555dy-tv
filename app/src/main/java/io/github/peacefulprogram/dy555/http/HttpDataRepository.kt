package io.github.peacefulprogram.dy555.http

import cn.hutool.crypto.digest.HMac
import cn.hutool.crypto.digest.HmacAlgorithm
import cn.hutool.crypto.digest.MD5
import com.google.gson.Gson
import io.github.peacefulprogram.dy555.Constants
import io.github.peacefulprogram.dy555.Dy555Application
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.and
import okhttp3.internal.toHexString
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

class HttpDataRepository(private val okHttpClient: OkHttpClient) {

    private fun getDocument(url: String): Document {
        val resp = Request.Builder()
            .url(url)
            .get()
            .build()
            .let {
                okHttpClient.newCall(it).execute()
            }
        val cookie = Cookie.parseAll(resp.request.url, resp.headers).find {
            it.name == "ge_ua_p"
        }?.value
        val html = resp
            .body!!
            .string()
        if (cookie == null) {
            return Jsoup.parse(html)
        }
        val geUaKey = requestGeUaKey(cookie, html)
        Dy555Application.ge_ua_key = geUaKey
        return getDocument(url)
    }

    private fun requestGeUaKey(geUaP: String, html: String): String {
        val idx = html.indexOf(" nonce")
        var numStart = -1
        var nonceStr = ""
        for (i in (idx + 7) until html.length) {
            val isDigit = html[i].isDigit()
            if (numStart == -1 && isDigit) {
                numStart = i
                continue
            }
            if (numStart != -1 && !isDigit) {
                nonceStr = html.substring(startIndex = numStart, i)
                break
            }
        }
        if (nonceStr.isEmpty()) {
            throw RuntimeException("未找到nonce")
        }
        val nonce = nonceStr.toLong()
        var sum = 0L
        geUaP.forEachIndexed { charIndex, ch ->
            if (ch in '0'..'9' || ch in 'a'..'z' || ch in 'A'..'Z') {
                sum += ch.code * (nonce + charIndex)
            }
        }
        val body = FormBody.Builder()
            .add("nonce", nonceStr)
            .add("sum", sum.toString())
            .build()
        val resp = Request.Builder()
            .url(Constants.BASE_URL)
            .post(body)
            .addHeader("cookie", "ge_ua_p=$geUaP")
            .addHeader("X-Ge-Ua-Step", "prev")
            .build()
            .let {
                okHttpClient.newCall(it).execute()
            }
        return Cookie.parseAll(resp.request.url, resp.headers)
            .find { it.name == "ge_ua_key" }
            ?.value
            ?: throw RuntimeException("未获取到ge_ua_key")

    }

    private fun getIdFromUrl(url: String): String =
        url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'))

    fun getHomePage(): VideosOfType {
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
        return VideosOfType(
            recommendVideos = wideRecommends,
            ranks = emptyList(),
            videoGroups = resultList
        )
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

    fun getVideoPageByType(typeId: Int): VideosOfType {
        val document = getDocument("${Constants.BASE_URL}/vodtype/${typeId}.html")
        val modules = document.getElementsByClass("module")
        var recommend = emptyList<MediaCardData>()
        val groups = mutableListOf<Pair<String, List<MediaCardData>>>()
        var ranks = emptyList<Pair<String, List<MediaCardData>>>()
        for (module in modules) {
            val moduleTitle =
                module.getElementsByClass("module-title")
                    .firstOrNull()
                    ?.children()
                    ?.firstOrNull()
                    ?.textNodes()
                    ?.firstOrNull()
                    ?.text() ?: "推荐"
            if ("排行榜" == moduleTitle) {
                val rankVideos = module.getElementsByClass("tab-list")
                val rankNames =
                    module.getElementsByClass("module-tab-item").map { it.text().trim() }
                val rankCount = rankVideos.size.coerceAtMost(rankNames.size)
                ranks = (0 until rankCount).map { index ->
                    Pair(
                        rankNames[index],
                        rankVideos[index].select("a").map(this::parseVideoLinkElement)
                    )
                }
            } else {
                val videos = module.select(".module-items > a").map(this::parseVideoLinkElement)
                if (moduleTitle == "推荐") {
                    recommend = videos
                } else {
                    groups.add(Pair(moduleTitle, videos))
                }
            }
        }
        return VideosOfType(
            recommendVideos = recommend,
            videoGroups = groups,
            ranks = ranks
        )
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
        val pageContainer = document.getElementById("page") ?: return false
        val currentPage =
            pageContainer.getElementsByClass("page-current").firstOrNull()?.text()?.trim()
                ?: return false
        val lastPageHref =
            pageContainer.child(pageContainer.childrenSize() - 1).attr("href") ?: return false
        return !lastPageHref.endsWith("/$currentPage.html")
    }

    fun queryVideoUrl(episodeId: String): String? {
        val url = episodeId.split("-").run {
            "${Constants.BASE_URL}/voddisp/id/${get(0)}/sid/${get(1)}/nid/${get(2)}.html"
        }
        val encryptUrl = Request.Builder().url(url).get().build()
            .run { okHttpClient.newCall(this).execute() }.body?.string()
            ?.let { Gson().fromJson(it, Map::class.java)["url"] as String? }
            ?: throw RuntimeException("加密url为空")

        val serverUrl = Constants.PLAY_URL_SERVER
        val timestamp = System.currentTimeMillis() / 0x3e8
        val iv = "d11324dcscfe16c0".toByteArray(Charsets.UTF_8)
        val key = "55cc5c42a943afdc".toByteArray(Charsets.UTF_8)
        val keySpec = SecretKeySpec(key, "AES")
        val packString = Cipher.getInstance("AES/CBC/PKCS5Padding").run {
            init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
            doFinal(encryptUrl.toByteArray())
        }.joinToString(separator = "", transform = ::byteToHexString).uppercase()
        val urlMd5 = MD5.create()
            .digestHex(serverUrl + "GET" + timestamp + "55ca5c4d11424dcecfe16c08a943afdc")
        val signature = HMac(HmacAlgorithm.HmacSHA256, urlMd5.toByteArray()).digestHex(packString)
        val requestUrl = "${Constants.PLAY_URL_SERVER}/get_play_url"
        val accessToken =
            MD5.create().digestHex(requestUrl.substring(requestUrl.indexOf("://") + 3))
        return Request.Builder()
            .url("$requestUrl?app_key=$appKey&client_key=$clientKey&request_token=$requestToken&access_token=$accessToken")
            .header("X-PLAYER-TIMESTAMP", timestamp.toString())
            .header("X-PLAYER-SIGNATURE", signature)
            .header("X-PLAYER-METHOD", "GET")
            .header("X-PLAYER-PACK", packString).build().let {
                okHttpClient.newCall(it).execute()
            }.body?.string()?.let {
                // aes解密
                val json = Cipher.getInstance("AES/CBC/PKCS5Padding").run {
                    init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
                    doFinal(it.chunked(2).map { it.toInt(0x10).toByte() }.toByteArray())
                }.toString(Charsets.UTF_8)
                Gson().fromJson<Map<String, Map<String, String>>>(
                    json, Map::class.java
                )["data"]?.get("url")
            }
    }

    private fun byteToHexString(byte: Byte): String {
        val result = (byte and 0xff).toHexString()
        if (result.length == 1) {
            return "0$result"
        }
        return result
    }

    fun queryCategory(param: List<String>, page: Int): PageResult<MediaCardData> {
        val paramStr = param.toMutableList().run {
            this[8] = page.toString() // 第9个参数是页码
            this.joinToString(separator = "-")
        }
        val doc = getDocument("${Constants.BASE_URL}/vodshow/$paramStr.html")
        val videos =
            doc.getElementsByClass("module-items")[0].children().map(this::parseVideoLinkElement)
        return PageResult(videos, page, hasNextPage(doc))
    }

    fun queryCategoryFilter(param: List<String>): List<Triple<Int, String, List<Pair<String, String>>>> {
        val pathParam = param.joinToString(separator = "-")
        val doc = getDocument("${Constants.BASE_URL}/vodshow/$pathParam.html")
        val groups = doc.selectFirst(".module-main.module-class")!!.select(".module-class-item")
        val result = ArrayList<Triple<Int, String, List<Pair<String, String>>>>(groups.size)
        val getLastPathSegment = { url: String ->
            url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'))
        }
        groups.forEach { group ->
            val groupName = group.selectFirst(".module-item-title")!!.text().trim()
            val allConditions = group.select(".module-item-box a")
            if (allConditions.isEmpty()) {
                return@forEach
            }
            val firstCond = getLastPathSegment(allConditions[0].attr("href")).split('-')
            val another =
                if (allConditions.size > 1) getLastPathSegment(allConditions[1].attr("href")).split(
                    '-'
                ) else param
            // 比较第一个和第二个不同的部分 确定当前分类的参数位置
            var paramIndex = -1
            for (i in firstCond.indices) {
                if (firstCond[i] != another[i]) {
                    paramIndex = i
                    break
                }
            }
            if (paramIndex == -1) {
                return@forEach
            }
            val filters = allConditions.map { link ->
                Pair(
                    link.text().trim(), getLastPathSegment(link.attr("href")).split('-')[paramIndex]
                )
            }
            result.add(Triple(paramIndex, groupName, filters))
        }
        return result

    }

    fun loadVideoServerUrl(): String {
        val doc = getDocument("${Constants.BASE_URL}/player.html?v=1")
        val scripts = doc.select("script")
        var result: String? = null
        val pattern = Pattern.compile("server_url\\s*=\\s*['\"]([\\w:/.]+)['\"]")
        for (script in scripts) {
            val html = script.html()
            if (html.isEmpty()) {
                continue
            }
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                result = matcher.group(1)
                break
            }
        }
        return result ?: ""
    }

    fun searchVideo(searchParam: List<String>, pageNumber: Int): PageResult<MediaCardData> {
        val pathParam = searchParam.toMutableList().apply {
            this[10] = pageNumber.toString()
        }.joinToString(separator = "-")
        val doc = getDocument("${Constants.BASE_URL}/vodsearch/$pathParam.html")
        val videos = doc.select(".module-card-item").map { card ->
            val id = card.selectFirst("a")!!.attr("href").run {
                substring(lastIndexOf('/') + 1, lastIndexOf('.'))
            }
            MediaCardData(
                id = id,
                title = card.selectFirst(".module-card-item-title")!!.text().trim(),
                pic = card.selectFirst("img")!!.dataset()["original"]!!,
                note = card.selectFirst(".module-item-note")?.text()?.trim(),
            )
        }
        return PageResult(
            data = videos, page = pageNumber, hasNextPage = hasNextPage(doc)
        )
    }

    fun querySearchRecommend(): List<String> {
        val doc = getDocument(Constants.BASE_URL)
        return doc.selectFirst(".search-recommend")!!.select("a").map { link ->
            link.text().trim()
        }
    }

    companion object {

        val appKey by lazy {
            MD5.create().digestHex("www.555dy.com")
        }

        val clientKey by lazy {
            MD5.create().digestHex(Constants.USER_AGENT)
        }

        val requestToken by lazy {
            MD5.create().digestHex("https://zyz.sdljwomen.com")
        }

    }
}