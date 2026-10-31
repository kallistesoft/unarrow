package ca.kallistesoft.unarrow.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The app ships in English, French (France) and French (Canada). values-fr must translate every string; values-fr-rCA
 * only holds the strings Canada says differently, and falls back to values-fr for the rest.
 */
class TranslationsTest {
    private val english = keys("values")
    private val france = keys("values-fr")
    private val canada = keys("values-fr-rCA")

    @Test
    fun `every string is translated for France`() {
        assertEquals("missing in values-fr", emptySet<String>(), english.keys - france.keys)
        assertEquals("not in values (remove)", emptySet<String>(), france.keys - english.keys)
    }

    @Test
    fun `Canada only overrides existing strings`() {
        assertEquals("not in values", emptySet<String>(), canada.keys - english.keys)
    }

    @Test
    fun `translations keep the placeholders`() {
        for ((name, text) in english) {
            val expected = placeholders(text)
            for ((locale, strings) in listOf("fr" to france, "fr-rCA" to canada)) {
                val translated = strings[name] ?: continue
                assertEquals("$locale/$name", expected, placeholders(translated))
            }
        }
    }

    @Test
    fun `there is something to translate`() {
        assertTrue(english.size > 50)
    }

    private fun placeholders(text: String) = Regex("%(\\d\\$)?[ds]").findAll(text).map { it.value }.sorted().toList()

    /** Name → text of every string and plural item (`name#quantity`) in res/<folder>/strings.xml. */
    private fun keys(folder: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/$folder/strings.xml"))
        val result = mutableMapOf<String, String>()
        val strings = doc.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val node = strings.item(i)
            result[node.attributes.getNamedItem("name").nodeValue] = node.textContent
        }
        val plurals = doc.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val node = plurals.item(i)
            val name = node.attributes.getNamedItem("name").nodeValue
            val items = node.childNodes
            for (j in 0 until items.length) {
                val item = items.item(j)
                if (item.nodeName == "item") result[name] = item.textContent // One entry per plural is enough.
            }
        }
        return result
    }
}
