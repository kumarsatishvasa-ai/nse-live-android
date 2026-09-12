```kotlin
package com.nselive.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NseApi {

    companion object {
        private const val BASE_URL = "https://www.nseindia.com"

        private const val OPTION_CHAIN_URL =
            "$BASE_URL/api/option-chain-v3"

        private const val ALL_INDICES_URL =
            "$BASE_URL/api/allIndices"

        private const val CONTRACT_INFO_URL =
            "$BASE_URL/api/option-chain-contract-info"
    }

    private val cookies = mutableMapOf<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {

        override fun saveFromResponse(
            url: HttpUrl,
            cookies: List<Cookie>
        ) {
            this@NseApi.cookies[url.host] =
                cookies.toMutableList()
        }

        override fun loadForRequest(
            url: HttpUrl
        ): List<Cookie> {
            return this@NseApi.cookies[url.host]
                ?.filter { it.expiresAt > System.currentTimeMillis() }
                ?: emptyList()
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val headers = mapOf(
        "Accept" to "application/json, text/plain, */*",
        "Accept-Language" to "en-US,en;q=0.9",
        "Cache-Control" to "no-cache",
        "Pragma" to "no-cache",
        "X-Requested-With" to "XMLHttpRequest",
        "User-Agent" to
                "Mozilla/5.0 (Linux; Android 14) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0 Mobile Safari/537.36"
    )

    private suspend fun get(
        url: String,
        referer: String
    ): String = withContext(Dispatchers.IO) {

        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Referer", referer)

        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        val response = client.newCall(
            requestBuilder.build()
        ).execute()

        if (!response.isSuccessful) {
            throw NseApiException(
                "NSE HTTP ${response.code}"
            )
        }

        response.body?.string()
            ?: throw NseApiException(
                "NSE returned an empty response"
            )
    }

    private suspend fun prime() {
        try {
            get(
                "$BASE_URL/",
                BASE_URL + "/"
            )

            get(
                "$BASE_URL/option-chain",
                BASE_URL + "/option-chain"
            )
        } catch (e: Exception) {
            // Priming failure is handled by the actual request.
        }
    }

    suspend fun getOptionChain(
        symbol: String = "NIFTY",
        expiry: String
    ): OptionChain {

        prime()

        val timestamp =
            System.currentTimeMillis()

        val url =
            "$OPTION_CHAIN_URL" +
                    "?type=Indices" +
                    "&symbol=${encode(symbol)}" +
                    "&expiry=${encode(expiry)}" +
                    "&_=$timestamp"

        val json = get(
            url,
            "$BASE_URL/option-chain"
        )

        return parseOptionChain(json)
    }

    suspend fun getExpiries(
        symbol: String = "NIFTY"
    ): List<String> {

        prime()

        val url =
            "$CONTRACT_INFO_URL" +
                    "?symbol=${encode(symbol)}" +
                    "&_=${System.currentTimeMillis()}"

        val json = get(
            url,
            "$BASE_URL/option-chain"
        )

        val root = JSONObject(json)

        val result = mutableListOf<String>()

        val expiryArray =
            root.optJSONArray("expiryDates")

        if (expiryArray != null) {
            for (i in 0 until expiryArray.length()) {
                result.add(
                    expiryArray.optString(i)
                )
            }
        }

        if (result.isEmpty()) {
            val records =
                root.optJSONObject("records")

            val recordsExpiry =
                records?.optJSONArray("expiryDates")

            if (recordsExpiry != null) {
                for (i in 0 until recordsExpiry.length()) {
                    result.add(
                        recordsExpiry.optString(i)
                    )
                }
            }
        }

        if (result.isEmpty()) {
            throw NseApiException(
                "NSE returned no expiry dates"
            )
        }

        return result
    }

    suspend fun getIndiaVix(): IndiaVix {

        prime()

        val url =
            "$ALL_INDICES_URL" +
                    "?_=${System.currentTimeMillis()}"

        val json = get(
            url,
            "$BASE_URL/market-data/live-market-indices"
        )

        val root = JSONObject(json)

        val data =
            root.optJSONArray("data")
                ?: throw NseApiException(
                    "NSE VIX response has no data"
                )

        for (i in 0 until data.length()) {

            val row = data.optJSONObject(i)
                ?: continue

            val index =
                row.optString("index")
                    .trim()
                    .uppercase(Locale.US)

            if (index == "INDIA VIX") {

                return IndiaVix(
                    value = row.optDouble(
                        "last",
                        0.0
                    ),
                    changePct = row.optDouble(
                        "percentChange",
                        0.0
                    ),
                    open = row.optDouble(
                        "open",
                        0.0
                    )
                )
            }
        }

        throw NseApiException(
            "India VIX was not found"
        )
    }

    private fun parseOptionChain(
        json: String
    ): OptionChain {

        val root = JSONObject(json)

        val records =
            root.optJSONObject("records")
                ?: throw NseApiException(
                    "NSE response has no records"
                )

        val timestamp =
            records.optString(
                "timestamp",
                now()
            )

        val underlying =
            records.optDouble(
                "underlyingValue",
                0.0
            )

        val data =
            records.optJSONArray("data")
                ?: throw NseApiException(
                    "NSE response has no option-chain data"
                )

        val contracts =
            mutableListOf<OptionContract>()

        for (i in 0 until data.length()) {

            val row =
                data.optJSONObject(i)
                    ?: continue

            val strike =
                row.optDouble(
                    "strikePrice",
                    0.0
                )

            val expiry =
                row.optString(
                    "expiryDate",
                    ""
                )

            if (strike <= 0.0) {
                continue
            }

            val ce =
                row.optJSONObject("CE")

            val pe =
                row.optJSONObject("PE")

            contracts.add(
                OptionContract(
                    strikePrice = strike,
                    expiryDate = expiry,

                    callOi = ce?.optDouble(
                        "openInterest",
                        0.0
                    ) ?: 0.0,

                    callOiChange = ce?.optDouble(
                        "changeinOpenInterest",
                        0.0
                    ) ?: 0.0,

                    callVolume = ce?.optDouble(
                        "totalTradedVolume",
                        0.0
                    ) ?: 0.0,

                    callIv = ce?.optDouble(
                        "impliedVolatility",
                        0.0
                    ) ?: 0.0,

                    callLtp = ce?.optDouble(
                        "lastPrice",
                        0.0
                    ) ?: 0.0,

                    putOi = pe?.optDouble(
                        "openInterest",
                        0.0
                    ) ?: 0.0,

                    putOiChange = pe?.optDouble(
                        "changeinOpenInterest",
                        0.0
                    ) ?: 0.0,

                    putVolume = pe?.optDouble(
                        "totalTradedVolume",
                        0.0
                    ) ?: 0.0,

                    putIv = pe?.optDouble(
                        "impliedVolatility",
                        0.0
                    ) ?: 0.0,

                    putLtp = pe?.optDouble(
                        "lastPrice",
                        0.0
                    ) ?: 0.0
                )
            )
        }

        if (contracts.isEmpty()) {
            throw NseApiException(
                "NSE returned no option contracts"
            )
        }

        return OptionChain(
            underlyingValue = underlying,
            timestamp = timestamp,
            contracts = contracts
        )
    }

    private fun encode(
        value: String
    ): String {
        return java.net.URLEncoder
            .encode(
                value,
                "UTF-8"
            )
    }

    private fun now(): String {
        return SimpleDateFormat(
            "dd-MMM-yyyy HH:mm:ss",
            Locale.US
        ).format(Date())
    }
}

class NseApiException(
    message: String
) : RuntimeException(message)
```
