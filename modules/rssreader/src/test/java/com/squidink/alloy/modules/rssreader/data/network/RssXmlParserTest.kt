package com.squidink.alloy.modules.rssreader.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class RssXmlParserTest {

    @Test
    fun `parses standard RSS 2 0 feed correctly`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>Tech News Feed</title>
                    <link>https://example.com</link>
                    <description>Latest tech stories</description>
                    <item>
                        <title>Kotlin 2.0 Released</title>
                        <link>https://example.com/kotlin-2</link>
                        <description><![CDATA[<p>JetBrains announced Kotlin 2.0 compiler with major speedups.</p>]]></description>
                        <pubDate>Tue, 29 Sep 2026 19:00:00 GMT</pubDate>
                        <author>Android Team</author>
                        <enclosure url="https://example.com/thumb.jpg" type="image/jpeg" />
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val parsed = RssXmlParser.parse(ByteArrayInputStream(xml.toByteArray()), "https://example.com/rss")
        assertEquals("Tech News Feed", parsed.title)
        assertEquals("Latest tech stories", parsed.description)
        assertEquals(1, parsed.items.size)

        val item = parsed.items.first()
        assertEquals("Kotlin 2.0 Released", item.title)
        assertEquals("https://example.com/kotlin-2", item.link)
        assertEquals("JetBrains announced Kotlin 2.0 compiler with major speedups.", item.description)
        assertEquals("https://example.com/thumb.jpg", item.imageUrl)
        assertEquals("Android Team", item.author)
        assertTrue(item.pubDate > 0)
    }

    @Test
    fun `parses Atom 1 0 feed correctly`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <title>Atom Tech Feed</title>
                <subtitle>Atom subtitle</subtitle>
                <entry>
                    <title>Compose 1.8 Stable</title>
                    <link rel="alternate" href="https://android.com/compose-1.8" />
                    <summary>Jetpack Compose updates.</summary>
                    <published>2026-09-29T19:00:00Z</published>
                    <author><name>Google</name></author>
                </entry>
            </feed>
        """.trimIndent()

        val parsed = RssXmlParser.parse(ByteArrayInputStream(xml.toByteArray()), "https://android.com/feed.atom")
        assertEquals("Atom Tech Feed", parsed.title)
        assertEquals("Atom subtitle", parsed.description)
        assertEquals(1, parsed.items.size)

        val item = parsed.items.first()
        assertEquals("Compose 1.8 Stable", item.title)
        assertEquals("https://android.com/compose-1.8", item.link)
        assertEquals("Jetpack Compose updates.", item.description)
        assertEquals("Google", item.author)
        assertTrue(item.pubDate > 0)
    }

    @Test
    fun `parses RSS 2 0 feed with media content image correctly`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
                <channel>
                    <title>Ars Technica</title>
                    <link>https://arstechnica.com</link>
                    <description>Tech stories</description>
                    <item>
                        <title>Superconducting breakthrough</title>
                        <link>https://arstechnica.com/breakthrough</link>
                        <description>Scientists announced breakthrough</description>
                        <pubDate>Tue, 29 Sep 2026 22:30:37 +0000</pubDate>
                        <media:content url="https://cdn.arstechnica.net/image.jpg" medium="image" type="image/jpeg" />
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val parsed = RssXmlParser.parse(ByteArrayInputStream(xml.toByteArray()), "https://arstechnica.com/feed")
        assertEquals(1, parsed.items.size)
        val item = parsed.items.first()
        assertEquals("Superconducting breakthrough", item.title)
        assertEquals("https://cdn.arstechnica.net/image.jpg", item.imageUrl)
    }

    @Test
    fun `xxe external entity injection does not resolve`() {
        val xmlWithXxe = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE rss [
                <!ENTITY xxe SYSTEM "file:///etc/passwd">
            ]>
            <rss version="2.0">
                <channel>
                    <title>&xxe;</title>
                    <item>
                        <title>Test Item</title>
                        <description>&xxe;</description>
                    </item>
                </channel>
            </rss>
        """.trimIndent()

        val parsed = RssXmlParser.parse(ByteArrayInputStream(xmlWithXxe.toByteArray()), "https://evil.com/feed")
        // The title and description should NOT contain the secret /etc/passwd contents
        assertTrue(parsed.title == null || !parsed.title.contains("root:"))
        val item = parsed.items.firstOrNull()
        assertTrue(item == null || item.description == null || !item.description.contains("root:"))
    }
}
