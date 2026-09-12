package com.nselive.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
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

        private const val OPTION_CHAIN_PAGE =
            "$BASE_URL/option-chain"

        private const val HOME_PAGE =
            "$BASE_URL/"
    }

    /*
     * NSE uses cookies/session information.
     * Keep cookies received from NSE and send them
     * with subsequent requests.
     */
    private val cookies =
        mutableMapOf<String, MutableList<Cookie>>()

    private val cookieJar =
        object : CookieJar {

            override fun saveFromResponse(
                url: HttpUrl,
                cookies: List<Cookie>
            ) {
                if (cookies.isNotEmpty()) {
                    this@NseApi.cookies[url.host] =
                        cookies.toMutableList()
                }
            }

            override fun loadForRequest(
    url: HttpUrl
): List<Cookie> {
    return this@NseApi.cookies[url.host]
        ?.filter { cookie ->
            cookie.expiresAt > System.currentTimeMillis()
        }
        ?: emptyList()
}

                }
            }
        }

    /*
     * HTTP client
     */
    private val client =
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(
                20,
                TimeUnit.SECONDS
            )
            .readTimeout(
                30,
                TimeUnit.SECONDS
            )
            .writeTimeout(
                30,
                TimeUnit.SECONDS
            )
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

    /*
     * Headers that make the request look like
     * a normal browser request.
     */
    private val headers =
        mapOf(

            "Accept" to
                    "application/json, text/plain, */*",

            "Accept-Language" to
                    "en-US,en;q=0.9",

            "Cache-Control" to
                    "no-cache",

            "Pragma" to
                    "no-cache",

            "User-Agent" to
                    "Mozilla/5.0 (Linux; Android 14; " +
                    "Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Mobile Safari/537.36",

            "Sec-Fetch-Dest" to
                    "empty",

            "Sec-Fetch-Mode" to
                    "cors",

            "Sec-Fetch-Site" to
                    "same-origin",

            "X-Requested-With" to
                    "XMLHttpRequest"
        )

    /**
     * Generic GET request.
     */
    private suspend fun get(
        url: String,
        referer: String
    ): String = withContext(Dispatchers.IO) {

        val requestBuilder =
            Request.Builder()
                .url(url)
                .get()
                .header(
                    "Referer",
                    referer
                )

        headers.forEach { (key, value) ->
            requestBuilder.header(
                key,
                value
            )
        }

        val request =
            requestBuilder.build()

        val response =
            client
                .newCall(request)
                .execute()

        response.use {

            val body =
                it.body?.string()
                    ?: ""

            if (!it.isSuccessful) {

                throw NseApiException(
                    "NSE HTTP ${it.code}: ${body.take(300)}"
                )
            }

            if (body.isBlank()) {

                throw NseApiException(
                    "NSE returned an empty response"
                )
            }

            return@withContext body
        }
    }

    /**
     * Open NSE pages first so NSE can establish
     * the required session cookies.
     */
     private val cookies =
    mutableMapOf<String, MutableList<Cookie>>()

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

        val now =
            System.currentTimeMillis()

        return this@NseApi.cookies[url.host]
            ?.filter { cookie ->
                cookie.expiresAt > now
            }
            ?: emptyList()
    }
}

    private suspend fun prime() {

        try {

            get(
                HOME_PAGE,
                HOME_PAGE
            )

        } catch (e: Exception) {

            // Continue because the next request may
            // still succeed.
        }

        try {

            get(
                OPTION_CHAIN_PAGE,
                HOME_PAGE
            )

        } catch (e: Exception) {

            // Continue.
        }
    }

    /**
     * Get NIFTY option-chain data.
     */
    suspend fun getOptionChain(
        symbol: String = "NIFTY",
        expiry: String
    ): OptionChain {

        prime()

        val timestamp =
            System.currentTimeMillis()

        val url =
            OPTION_CHAIN_URL +
                    "?type=Indices" +
                    "&symbol=${encode(symbol)}" +
                    "&expiry=${encode(expiry)}" +
                    "&_=$timestamp"

        val json =
            get(
                url,
                OPTION_CHAIN_PAGE
            )

        return parseOptionChain(json)
    }

    /**
     * Get available NIFTY expiry dates.
     */
    suspend fun getExpiries(
        symbol: String = "NIFTY"
    ): List<String> {

        prime()

        val timestamp =
            System.currentTimeMillis()

        val url =
            CONTRACT_INFO_URL +
                    "?symbol=${encode(symbol)}" +
                    "&_=$timestamp"

        val json =
            get(
                url,
                OPTION_CHAIN_PAGE
            )

        val root =
            try {
                JSONObject(json)
            } catch (e: Exception) {

                throw NseApiException(
                    "Invalid NSE expiry response: " +
                            "${e.message}"
                )
            }

        val result =
            mutableListOf<String>()

        /*
         * Possible response:
         *
         * {
         *   "expiryDates": [...]
         * }
         */
        val expiryArray =
            root.optJSONArray(
                "expiryDates"
            )

        if (expiryArray != null) {

            for (i in 0 until expiryArray.length()) {

                val expiry =
                    expiryArray.optString(i)
                        .trim()

                if (expiry.isNotEmpty()) {
                    result.add(expiry)
                }
            }
        }

        /*
         * Some NSE responses place expiryDates
         * inside records.
         */
        if (result.isEmpty()) {

            val records =
                root.optJSONObject(
                    "records"
                )

            val recordsExpiry =
                records?.optJSONArray(
                    "expiryDates"
                )

            if (recordsExpiry != null) {

                for (
                    i in 0 until recordsExpiry.length()
                ) {

                    val expiry =
                        recordsExpiry
                            .optString(i)
                            .trim()

                    if (expiry.isNotEmpty()) {
                        result.add(expiry)
                    }
                }
            }
        }

        if (result.isEmpty()) {

            throw NseApiException(
                "NSE returned no NIFTY expiry dates"
            )
        }

        return result.distinct()
    }

    /**
     * Get India VIX from NSE all-indices endpoint.
     */
    suspend fun getIndiaVix(): IndiaVix {

        prime()

        val timestamp =
            System.currentTimeMillis()

        val url =
            ALL_INDICES_URL +
                    "?_=$timestamp"

        val json =
            get(
                url,
                "$BASE_URL/market-data/" +
                        "live-market-indices"
            )

        val root =
            try {
                JSONObject(json)
            } catch (e: Exception) {

                throw NseApiException(
                    "Invalid NSE indices response: " +
                            "${e.message}"
                )
            }

        val data =
            root.optJSONArray("data")
                ?: throw NseApiException(
                    "NSE indices response has no data"
                )

        for (i in 0 until data.length()) {

            val row =
                data.optJSONObject(i)
                    ?: continue

            val indexName =
                row.optString(
                    "index",
                    ""
                )
                    .trim()
                    .uppercase(Locale.US)

            if (
                indexName == "INDIA VIX" ||
                indexName == "INDIA VIX INDEX"
            ) {

                val last =
                    getDouble(
                        row,
                        "last"
                    )

                val percentChange =
                    getDouble(
                        row,
                        "percentChange"
                    )

                val open =
                    getDouble(
                        row,
                        "open"
                    )

                if (last <= 0.0) {

                    throw NseApiException(
                        "NSE returned invalid India VIX value"
                    )
                }

                return IndiaVix(
                    value = last,
                    changePct = percentChange,
                    open = open
                )
            }
        }

        throw NseApiException(
            "India VIX was not found in NSE response"
        )
    }

    /**
     * Parse NSE option-chain JSON.
     */
    private fun parseOptionChain(
        json: String
    ): OptionChain {

        val root =
            try {
                JSONObject(json)
            } catch (e: Exception) {

                throw NseApiException(
                    "Invalid NSE option-chain JSON: " +
                            "${e.message}"
                )
            }

        val records =
            root.optJSONObject(
                "records"
            )
                ?: throw NseApiException(
                    "NSE response has no records"
                )

        val timestamp =
            records.optString(
                "timestamp",
                now()
            )

        val underlying =
            getDouble(
                records,
                "underlyingValue"
            )

        if (underlying <= 0.0) {

            throw NseApiException(
                "NSE returned invalid NIFTY price"
            )
        }

        val data =
            records.optJSONArray(
                "data"
            )
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
                getDouble(
                    row,
                    "strikePrice"
                )

            val expiry =
                row.optString(
                    "expiryDate",
                    ""
                )
                    .trim()

            if (strike <= 0.0) {
                continue
            }

            /*
             * Call data
             */
            val ce =
                row.optJSONObject("CE")

            val callOi =
                getDouble(
                    ce,
                    "openInterest"
                )

            val callOiChange =
                getDouble(
                    ce,
                    "changeinOpenInterest"
                )

            val callVolume =
                getDouble(
                    ce,
                    "totalTradedVolume"
                )

            val callIv =
                getDouble(
                    ce,
                    "impliedVolatility"
                )

            val callLtp =
                getDouble(
                    ce,
                    "lastPrice"
                )

            /*
             * Put data
             */
            val pe =
                row.optJSONObject("PE")

            val putOi =
                getDouble(
                    pe,
                    "openInterest"
                )

            val putOiChange =
                getDouble(
                    pe,
                    "changeinOpenInterest"
                )

            val putVolume =
                getDouble(
                    pe,
                    "totalTradedVolume"
                )

            val putIv =
                getDouble(
                    pe,
                    "impliedVolatility"
                )

            val putLtp =
                getDouble(
                    pe,
                    "lastPrice"
                )

            /*
             * Ignore completely empty contracts.
             */
            if (
                callOi == 0.0 &&
                putOi == 0.0 &&
                callVolume == 0.0 &&
                putVolume == 0.0
            ) {
                continue
            }

            contracts.add(
                OptionContract(

                    strikePrice =
                        strike,

                    expiryDate =
                        expiry,

                    callOi =
                        callOi,

                    callOiChange =
                        callOiChange,

                    callVolume =
                        callVolume,

                    callIv =
                        callIv,

                    callLtp =
                        callLtp,

                    putOi =
                        putOi,

                    putOiChange =
                        putOiChange,

                    putVolume =
                        putVolume,

                    putIv =
                        putIv,

                    putLtp =
                        putLtp
                )
            )
        }

        if (contracts.isEmpty()) {

            throw NseApiException(
                "NSE returned zero option contracts"
            )
        }

        return OptionChain(

            underlyingValue =
                underlying,

            timestamp =
                timestamp,

            contracts =
                contracts
        )
    }

    /**
     * Safely read a numeric value from JSON.
     *
     * NSE can occasionally return null,
     * empty strings, or numbers as strings.
     */
    private fun getDouble(
        obj: JSONObject?,
        key: String
    ): Double {

        if (obj == null) {
            return 0.0
        }

        val value =
            obj.opt(key)

        return when (value) {

            is Number ->
                value.toDouble()

            is String ->
                value
                    .trim()
                    .replace(",", "")
                    .toDoubleOrNull()
                    ?: 0.0

            else ->
                0.0
        }
    }

    /**
     * URL encode query parameters.
     */
    private fun encode(
        value: String
    ): String {

        return URLEncoder.encode(
            value,
            "UTF-8"
        )
    }

    /**
     * Current timestamp for fallback display.
     */
    private fun now(): String {

        return SimpleDateFormat(
            "dd-MMM-yyyy HH:mm:ss",
            Locale.US
        )
            .format(Date())
    }
}

class NseApiException(
    message: String
) : RuntimeException(message)
