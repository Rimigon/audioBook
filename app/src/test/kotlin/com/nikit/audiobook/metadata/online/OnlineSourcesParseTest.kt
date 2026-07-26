package com.nikit.audiobook.metadata.online

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnlineSourcesParseTest {
    @Test fun openLibrary_parseSample() {
        val json =
            """
            {
              "docs": [
                {
                  "title": "Dune",
                  "author_name": ["Frank Herbert"],
                  "text": "Set on the desert planet Arrakis...",
                  "cover_i": 12345
                }
              ]
            }
            """.trimIndent()
        val r = OpenLibrarySource().parse(json)
        assertThat(r).isNotNull()
        assertThat(r!!.title).isEqualTo("Dune")
        assertThat(r.author).isEqualTo("Frank Herbert")
        assertThat(r.coverUrl).contains("12345")
        assertThat(r.source).isEqualTo("OpenLibrary")
    }

    @Test fun openLibrary_emptyDocs_returnsNull() {
        val json = """{"docs": []}"""
        assertThat(OpenLibrarySource().parse(json)).isNull()
    }

    @Test fun googleBooks_parseSample() {
        val json =
            """
            {
              "items": [
                {
                  "volumeInfo": {
                    "title": "Dune",
                    "authors": ["Frank Herbert"],
                    "description": "A desert epic.",
                    "imageLinks": { "thumbnail": "http://img/dune.jpg" }
                  }
                }
              ]
            }
            """.trimIndent()
        val r = GoogleBooksSource().parse(json)
        assertThat(r).isNotNull()
        assertThat(r!!.title).isEqualTo("Dune")
        assertThat(r.author).isEqualTo("Frank Herbert")
        assertThat(r.coverUrl).isEqualTo("http://img/dune.jpg")
        assertThat(r.source).isEqualTo("GoogleBooks")
    }

    @Test fun googleBooks_noItems_returnsNull() {
        assertThat(GoogleBooksSource().parse("""{"totalItems":0}""")).isNull()
    }
}
