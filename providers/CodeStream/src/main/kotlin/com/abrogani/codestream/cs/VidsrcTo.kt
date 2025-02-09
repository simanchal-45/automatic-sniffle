package com.abrogani.codestream.cs

import android.util.Base64
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.FileMoon
import com.lagradost.cloudstream3.extractors.Vidplay
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import java.net.URLDecoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class AnyVidplay(hostUrl: String) : Vidplay() {
    override val mainUrl = hostUrl
}

class VidsrcTo : ExtractorApi() {
    override val name = "VidSrcTo"
    override val mainUrl = "https://vidsrc.to"
    override val requiresReferer = true

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val mediaId = app.get(url).document.selectFirst("ul.episodes li a")?.attr("data-id")
        val res =
                app.get("$mainUrl/ajax/embed/episode/$mediaId/sources")
                        .parsedSafe<VidsrctoEpisodeSources>()
        if (res?.status == 200) {
            res.result?.apmap { source ->
                val embedRes =
                        app.get("$mainUrl/ajax/embed/source/${source.id}")
                                .parsedSafe<VidsrctoEmbedSource>()
                val finalUrl = DecryptUrl(embedRes?.result?.encUrl ?: "")
                when (source.title) {
                    "Vidplay" ->
                            AnyVidplay(finalUrl.substringBefore("/e/"))
                                    .getUrl(finalUrl, referer, subtitleCallback, callback)
                    "Filemoon" -> FileMoon().getUrl(finalUrl, referer, subtitleCallback, callback)
                    else -> {}
                }
            }
        }
    }

    private fun DecryptUrl(encUrl: String): String {
        var data = encUrl.toByteArray()
        data = Base64.decode(data, Base64.URL_SAFE)
        val rc4Key = SecretKeySpec("WXrUARXb1aDLaZjI".toByteArray(), "RC4")
        val cipher = Cipher.getInstance("RC4")
        cipher.init(Cipher.DECRYPT_MODE, rc4Key, cipher.parameters)
        data = cipher.doFinal(data)
        return URLDecoder.decode(data.toString(Charsets.UTF_8), "utf-8")
    }
    data class VidsrctoEpisodeSources(
            val status: Int,
            val result: List<VidsrctoResult>?
    )

    data class VidsrctoResult(
            val id: String,
            val title: String
    )

    data class VidsrctoEmbedSource(
            val status: Int,
            val result: VidsrctoUrl
    )

    data class VidsrctoUrl(val encUrl: String)
}
