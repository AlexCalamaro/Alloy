package com.squidink.alloy.modules.rssreader.data.network

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory

data class ParsedFeed(
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val items: List<ParsedFeedItem>
)

data class ParsedFeedItem(
    val id: String,
    val title: String,
    val description: String?,
    val link: String?,
    val author: String?,
    val pubDate: Long,
    val imageUrl: String?
)

/**
 * Robust XML parser supporting RSS 2.0 and Atom 1.0 feeds using standard Java SE DOM APIs.
 * Runs identically on host JVM unit tests and Android runtimes without stubbing issues.
 */
object RssXmlParser {

    fun parse(inputStream: InputStream, feedUrl: String): ParsedFeed {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            try {
                setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            } catch (_: Exception) {
                // Ignore unsupported features on host/unit-test JVM or older runtimes
            }
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(inputStream)
        val root = document.documentElement ?: return ParsedFeed(null, null, null, emptyList())
        root.normalize()

        val rootName = (root.localName ?: root.tagName ?: "").lowercase().substringAfter(":")

        return when (rootName) {
            "feed" -> parseAtomFeed(root, feedUrl)
            "rss", "rdf" -> parseRssFeed(root, feedUrl)
            "channel" -> parseChannel(root, root, feedUrl)
            else -> {
                root.findChild("channel")?.let { parseChannel(root, it, feedUrl) }
                    ?: root.findChild("feed")?.let { parseAtomFeed(it, feedUrl) }
                    ?: ParsedFeed(null, null, null, emptyList())
            }
        }
    }

    private fun parseRssFeed(root: Element, feedUrl: String): ParsedFeed {
        val channel = root.findChild("channel") ?: root
        return parseChannel(root, channel, feedUrl)
    }

    private fun parseChannel(root: Element, channel: Element, feedUrl: String): ParsedFeed {
        val title = channel.findChild("title")?.textContent?.trim()
        val description = channel.findChild("description")?.textContent?.trim()
        val imageUrl = channel.findChild("image")?.findChild("url")?.textContent?.trim()

        val itemsFromChannel = channel.findChildren("item")
        val items = (itemsFromChannel.ifEmpty { root.findChildren("item") }).mapNotNull {
            parseRssItem(it, feedUrl)
        }

        return ParsedFeed(
            title = title,
            description = description?.let { cleanHtml(it) },
            imageUrl = imageUrl,
            items = items
        )
    }

    private fun parseRssItem(item: Element, feedUrl: String): ParsedFeedItem? {
        val title = item.findChild("title")?.textContent?.trim().orEmpty()
        val link = item.findChild("link")?.textContent?.trim()
        val rawDesc = item.findChild("description")?.textContent
            ?: item.findChild("content:encoded", "encoded")?.textContent

        if (title.isBlank() && link.isNullOrBlank()) {
            return null
        }

        val pubDateStr = item.findChild("pubdate", "pubDate")?.textContent?.trim()
            ?: item.findChild("dc:date", "date")?.textContent?.trim()
        val pubDate = DateParser.parseDateToEpochMs(pubDateStr)

        val author = item.findChild("author")?.textContent?.trim()
            ?: item.findChild("dc:creator", "creator")?.textContent?.trim()

        val guid = item.findChild("guid")?.textContent?.trim()

        var imageUrl: String? = null

        // 1. Enclosure
        item.findChild("enclosure")?.let { enc ->
            val url = enc.getAttribute("url").takeIf { it.isNotBlank() }
            val type = enc.getAttribute("type")
            if (url != null && (type.isBlank() || type.startsWith("image/"))) {
                imageUrl = url
            }
        }

        // 2. Media content
        if (imageUrl == null) {
            item.findChild("media:content", "content")?.let { mc ->
                val url = mc.getAttribute("url").takeIf { it.isNotBlank() }
                val medium = mc.getAttribute("medium")
                val type = mc.getAttribute("type")
                if (url != null && (medium == "image" || type.startsWith("image/") || (medium.isBlank() && type.isBlank()))) {
                    imageUrl = url
                }
            }
        }

        // 3. Media thumbnail
        if (imageUrl == null) {
            imageUrl = item.findChild("media:thumbnail", "thumbnail")?.getAttribute("url")?.takeIf { it.isNotBlank() }
        }

        // 4. Media group (media:content or media:thumbnail inside media:group)
        if (imageUrl == null) {
            item.findChild("media:group", "group")?.let { mg ->
                val mc = mg.findChild("media:content", "content")
                val mt = mg.findChild("media:thumbnail", "thumbnail")
                imageUrl = mc?.getAttribute("url")?.takeIf { it.isNotBlank() }
                    ?: mt?.getAttribute("url")?.takeIf { it.isNotBlank() }
            }
        }

        // 5. itunes:image
        if (imageUrl == null) {
            imageUrl = item.findChild("itunes:image", "image")?.getAttribute("href")?.takeIf { it.isNotBlank() }
        }

        // 6. HTML fallback from description or content:encoded
        if (imageUrl == null) {
            val encodedContent = item.findChild("content:encoded", "encoded")?.textContent
            imageUrl = extractImageFromHtml(rawDesc) ?: extractImageFromHtml(encodedContent)
        }

        val identifier = guid ?: link ?: title
        val id = UUID.nameUUIDFromBytes("$feedUrl:$identifier".toByteArray()).toString()

        return ParsedFeedItem(
            id = id,
            title = if (title.isBlank()) "Untitled" else title,
            description = cleanHtml(rawDesc),
            link = link,
            author = author,
            pubDate = pubDate,
            imageUrl = imageUrl
        )
    }

    private fun parseAtomFeed(root: Element, feedUrl: String): ParsedFeed {
        val title = root.findChild("title")?.textContent?.trim()
        val description = root.findChild("subtitle")?.textContent?.trim()
        val imageUrl = root.findChild("logo")?.textContent?.trim()
            ?: root.findChild("icon")?.textContent?.trim()

        val items = root.findChildren("entry").mapNotNull { parseAtomEntry(it, feedUrl) }

        return ParsedFeed(
            title = title,
            description = description?.let { cleanHtml(it) },
            imageUrl = imageUrl,
            items = items
        )
    }

    private fun parseAtomEntry(entry: Element, feedUrl: String): ParsedFeedItem? {
        val title = entry.findChild("title")?.textContent?.trim().orEmpty()
        var link: String? = null
        var imageUrl: String? = null

        val links = entry.findChildren("link")
        for (l in links) {
            val rel = l.getAttribute("rel")
            val href = l.getAttribute("href").takeIf { it.isNotBlank() }
            val type = l.getAttribute("type")
            if (rel == "enclosure" && type.startsWith("image/") && href != null) {
                imageUrl = href
            } else if (href != null && (rel.isBlank() || rel == "alternate" || link == null)) {
                link = href
            }
        }

        val rawDesc = entry.findChild("summary")?.textContent
            ?: entry.findChild("content")?.textContent

        if (imageUrl == null) {
            imageUrl = entry.findChild("media:content", "content")?.getAttribute("url")?.takeIf { it.isNotBlank() }
                ?: entry.findChild("media:thumbnail", "thumbnail")?.getAttribute("url")?.takeIf { it.isNotBlank() }
        }

        if (imageUrl == null) {
            entry.findChild("media:group", "group")?.let { mg ->
                val mc = mg.findChild("media:content", "content")
                val mt = mg.findChild("media:thumbnail", "thumbnail")
                imageUrl = mc?.getAttribute("url")?.takeIf { it.isNotBlank() }
                    ?: mt?.getAttribute("url")?.takeIf { it.isNotBlank() }
            }
        }

        if (imageUrl == null) {
            imageUrl = entry.findChild("itunes:image", "image")?.getAttribute("href")?.takeIf { it.isNotBlank() }
        }

        if (imageUrl == null && rawDesc != null) {
            imageUrl = extractImageFromHtml(rawDesc)
        }

        val pubDateStr = entry.findChild("published")?.textContent?.trim()
            ?: entry.findChild("updated")?.textContent?.trim()
        val pubDate = DateParser.parseDateToEpochMs(pubDateStr)

        val author = entry.findChild("author")?.findChild("name")?.textContent?.trim()
        val idStr = entry.findChild("id")?.textContent?.trim()

        if (title.isBlank() && link.isNullOrBlank()) {
            return null
        }

        val identifier = idStr ?: link ?: title
        val id = UUID.nameUUIDFromBytes("$feedUrl:$identifier".toByteArray()).toString()

        return ParsedFeedItem(
            id = id,
            title = if (title.isBlank()) "Untitled" else title,
            description = cleanHtml(rawDesc),
            link = link,
            author = author,
            pubDate = pubDate,
            imageUrl = imageUrl
        )
    }

    private fun Element.findChild(vararg tagNames: String): Element? {
        val nodes = this.childNodes
        for (i in 0 until nodes.length) {
            val item = nodes.item(i)
            if (item.nodeType == Node.ELEMENT_NODE) {
                val nodeName = item.nodeName.lowercase()
                val localName = nodeName.substringAfter(":")
                for (tag in tagNames) {
                    val target = tag.lowercase()
                    if (nodeName == target || localName == target || target.substringAfter(":") == localName) {
                        return item as Element
                    }
                }
            }
        }
        return null
    }

    private fun Element.findChildren(vararg tagNames: String): List<Element> {
        val list = mutableListOf<Element>()
        val nodes = this.childNodes
        for (i in 0 until nodes.length) {
            val item = nodes.item(i)
            if (item.nodeType == Node.ELEMENT_NODE) {
                val nodeName = item.nodeName.lowercase()
                val localName = nodeName.substringAfter(":")
                for (tag in tagNames) {
                    val target = tag.lowercase()
                    if (nodeName == target || localName == target || target.substringAfter(":") == localName) {
                        list.add(item as Element)
                        break
                    }
                }
            }
        }
        return list
    }

    private fun cleanHtml(html: String?): String? {
        if (html == null) return null
        val stripped = html.replace(Regex("<[^>]*>"), " ").trim()
        val unescaped = stripped
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return unescaped.ifEmpty { null }
    }

    private fun extractImageFromHtml(html: String?): String? {
        if (html == null) return null
        val match = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.getOrNull(1)
    }
}
