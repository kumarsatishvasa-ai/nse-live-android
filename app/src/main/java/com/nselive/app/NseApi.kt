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

        private const val BASE_URL =
            "https://www.nseindia.com"

        private const val HOME_PAGE =
            "$BASE_URL/"

        private const val OPTION_CHAIN_PAGE =
            "$BASE_URL/option-chain"

        private const val OPTION_CHAIN_URL =
            "$BASE_URL/api/option-chain-v3"

        private const val ALL_INDICES_URL =
            "$BASE_URL/api/allIndices"

        private const val CONTRACT_INFO_URL =
            "$BASE_URL/api/option-chain-contract-info"
    }

    /*
     * ---------------------------------------------------------
     * COOKIE STORAGE
     * ---------------------------------------------------------
     */

    private val cookies =
        mutableMapOf<String, MutableList<Cookie>>()

    private val cookieJar =
        object : CookieJar {

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

                val savedCookies =
                    this@NseApi.cookies[url.host]
                        ?: return emptyList()

                val now =
                    System.currentTimeMillis()

                return savedCookies.filter {
                    it.expiresAt > now &&
                            !it.hostOnly ||
                            it.matches(url)
                }
            }
        }

    /*
     * ---------------------------------------------------------
     * HTTP CLIENT
     * ---------------------------------------------------------
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
     * ---------------------------------------------------------
     * NSE HEADERS
     * ---------------------------------------------------------
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
                    "Mozilla/5.0 " +
                    "(Linux; Android 14; Mobile) " +
                    "AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 " +
                    "Mobile Safari/537.36",

            "Sec-Fetch-Dest" to
                    "empty",

            "Sec-Fetch-Mode" to
                    "cors",

            "Sec-Fetch-Site" to
                    "same-origin"
        )

    /*
     * ---------------------------------------------------------
     * BASIC GET REQUEST
     * ---------------------------------------------------------
     */

    private suspend fun get(
        url: String,
        referer: String
    ): String =
        withContext(Dispatchers.IO) {

            val builder =
                Request.Builder()
                    .url(url)
                    .get()
                    .header(
                        "Referer",
                        referer
                    )

            headers.forEach { (key, value) ->
                builder.header(
                    key,
                    value
                )
            }

            val request =
                builder.build()

            client.newCall(request)
                .execute()
                .use { response ->

                    val body =
                        response.body?.string()
                            ?: ""

                    if (!response.isSuccessful) {

                        throw NseApiException(
                            "NSE HTTP ${response.code}: " +
                                    if (body.length > 300) {
                                        body.take(300)
                                    } else {
                                        body
                                    }
                        )
                    }

                    if (body.isBlank()) {
                        throw NseApiException(
                            "NSE returned an empty response"
                        )
                    }

                    body
                }
        }

    /*
     * ---------------------------------------------------------
     * PRIME NSE SESSION
     * ---------------------------------------------------------
     *
     * NSE may require cookies/session information before
     * requesting the API endpoint.
     * ---------------------------------------------------------
     */

    private suspend fun primeNseSession() {

        /*
         * First request NSE home page.
         */
        try {

            get(
                url = HOME_PAGE,
                referer = HOME_PAGE
            )

        } catch (e: Exception) {

            /*
             * Do not immediately fail.
             * The API request below may still work.
             */
        }

        /*
         * Second request option-chain page.
         */
        try {

            get(
                url = OPTION_CHAIN_PAGE,
                referer = HOME_PAGE
            )

        } catch (e: Exception) {

            /*
             * Actual API request will determine whether
             * the session is usable.
             */
        }
    }

    /*
     * ---------------------------------------------------------
     * GET EXPIRY DATES
     * ---------------------------------------------------------
     */

    suspend fun getExpiries(
        symbol: String = "NIFTY"
    ): List<String> {

        primeNseSession()

        val encodedSymbol =
            encode(symbol)

        val url =
            CONTRACT_INFO_URL +
                    "?symbol=$encodedSymbol" +
                    "&_=${System.currentTimeMillis()}"

        val json =
            get(
                url = url,
                referer = OPTION_CHAIN_PAGE
            )

        return parseExpiryDates(json)
    }

    /*
     * ---------------------------------------------------------
     * PARSE EXPIRY DATES
     * ---------------------------------------------------------
     */

    private fun parseExpiryDates(
        json: String
    ): List<String> {

        val root =
            try {
                JSONObject(json)
            } catch (e: Exception) {

                throw NseApiException(
                    "NSE expiry response is not JSON: " +
                            json.take(300)
                )
            }

        val result =
            mutableListOf<String>()

        /*
         * Possible format:
         *
         * {
         *   "expiryDates": [...]
         * }
         */

        val rootExpiry =
            root.optJSONArray(
                "expiryDates"
            )

        if (rootExpiry != null) {

            for (i in 0 until rootExpiry.length()) {

                val expiry =
                    rootExpiry.optString(
                        i,
                        ""
                    ).trim()

                if (expiry.isNotEmpty()) {
                    result.add(expiry)
                }
            }
        }

        /*
         * Possible format:
         *
         * {
         *   "records": {
         *       "expiryDates": [...]
         *   }
         * }
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
                        recordsExpiry.optString(
                            i,
                            ""
                        ).trim()

                    if (expiry.isNotEmpty()) {
                        result.add(expiry)
                    }
                }
            }
        }

        /*
         * Possible format:
         *
         * {
         *   "data": [...]
         * }
         *
         * Try to extract expiryDate from rows.
         */

        if (result.isEmpty()) {

            val data =
                root.optJSONArray("data")

            if (data != null) {

                for (
                    i in 0 until data.length()
                ) {

                    val row =
                        data.optJSONObject(i)
                            ?: continue

                    val expiry =
                        row.optString(
                            "expiryDate",
                            ""
                        ).trim()

                    if (
                        expiry.isNotEmpty() &&
                        !result.contains(expiry)
                    ) {
                        result.add(expiry)
                    }
                }
            }
        }

        if (result.isEmpty()) {

            throw NseApiException(
                "NSE returned no expiry dates. " +
                        "Response: ${json.take(500)}"
            )
        }

        return result
            .distinct()
    }

    /*
     * ---------------------------------------------------------
     * GET OPTION CHAIN
     * ---------------------------------------------------------
     */

    suspend fun getOptionChain(
        symbol: String = "NIFTY",
        expiry: String
    ): OptionChain {

        primeNseSession()

        val encodedSymbol =
            encode(symbol)

        val encodedExpiry =
            encode(expiry)

        val url =
            OPTION_CHAIN_URL +
                    "?type=Indices" +
                    "&symbol=$encodedSymbol" +
                    "&expiry=$encodedExpiry" +
                    "&_=${System.currentTimeMillis()}"

        val json =
            get(
                url = url,
                referer = OPTION_CHAIN_PAGE
            )

        return parseOptionChain(json)
    }

    /*
     * ---------------------------------------------------------
     * PARSE OPTION CHAIN
     * ---------------------------------------------------------
     */

    private fun parseOptionChain(
        json: String
    ): OptionChain {

        val root =
            try {
                JSONObject(json)
            } catch (e: Exception) {

                throw NseApiException(
                    "NSE option-chain response is not JSON: " +
                            json.take(500)
                )
            }

        val records =
            root.optJSONObject(
                "records"
            )
                ?: throw NseApiException(
                    "NSE response has no records. " +
                            "Response: ${json.take(500)}"
                )

        /*
         * Underlying NIFTY value.
         */

        val underlying =
            records.optDouble(
                "underlyingValue",
                0.0
            )

        /*
         * NSE timestamp.
         */

        val timestamp =
            records.optString(
                "timestamp",
                ""
            ).ifBlank {
                currentTime()
            }

        /*
         * Option-chain rows.
         */

        val data =
            records.optJSONArray(
                "data"
            )
                ?: throw NseApiException(
                    "NSE response has no option-chain data"
                )

        val contracts =
            mutableListOf<OptionContract>()

        for (
            i in 0 until data.length()
        ) {

            val row =
                data.optJSONObject(i)
                    ?: continue

            val strike =
                row.optDouble(
                    "strikePrice",
                    0.0
                )

            if (strike <= 0.0) {
                continue
            }

            val expiry =
                row.optString(
                    "expiryDate",
                    ""
                )

            /*
             * Call data.
             */

            val ce =
                row.optJSONObject("CE")

            /*
             * Put data.
             */

            val pe =
                row.optJSONObject("PE")

            val contract =
                OptionContract(

                    strikePrice =
                        strike,

                    expiryDate =
                        expiry,

                    callOi =
                        ce?.optDouble(
                            "openInterest",
                            0.0
                        ) ?: 0.0,

                    callOiChange =
                        ce?.optDouble(
                            "changeinOpenInterest",
                            0.0
                        ) ?: 0.0,

                    callVolume =
                        ce?.optDouble(
                            "totalTradedVolume",
                            0.0
                        ) ?: 0.0,

                    callIv =
                        ce?.optDouble(
                            "impliedVolatility",
                            0.0
                        ) ?: 0.0,

                    callLtp =
                        ce?.optDouble(
                            "lastPrice",
                            0.0
                        ) ?: 0.0,

                    putOi =
                        pe?.optDouble(
                            "openInterest",
                            0.0
                        ) ?: 0.0,

                    putOiChange =
                        pe?.optDouble(
                            "changeinOpenInterest",
                            0.0
                        ) ?: 0.0,

                    putVolume =
                        pe?.optDouble(
                            "totalTradedVolume",
                            0.0
                        ) ?: 0.0,

                    putIv =
                        pe?.optDouble(
                            "impliedVolatility",
                            0.0
                        ) ?: 0.0,

                    putLtp =
                        pe?.optDouble(
                            "lastPrice",
                            0.0
                        ) ?: 0.0
                )

            contracts.add(contract)
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

    /*
     * ---------------------------------------------------------
     * INDIA VIX
     * ---------------------------------------------------------
     */

    suspend fun getIndiaVix(): IndiaVix {

        primeNseSession()

        val url =
            ALL_INDICES_URL +
                    "?_=${System.currentTimeMillis()}"

        val json =
            get(
                url = url,
                referer =
                    "$BASE_URL/market-data/" +
                            "live-market-indices"
            )

        return parseIndiaVix(json)
    }

    /*
     * ---------------------------------------------------------
     * PARSE INDIA VIX
     * ---------------------------------------------------------
     */

    private fun parseIndiaVix(
        json: String
    ): IndiaVix {

        val root =
            try {
                JSONObject(json)
            } catch (e: Exception) {

                throw NseApiException(
                    "NSE VIX response is not JSON: " +
                            json.take(500)
                )
            }

        val data =
            root.optJSONArray(
                "data"
            )
                ?: throw NseApiException(
                    "NSE VIX response has no data"
                )

        for (
            i in 0 until data.length()
        ) {

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
                indexName.contains("INDIA VIX")
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

                return IndiaVix(

                    value =
                        last,

                    changePct =
                        percentChange,

                    open =
                        open
                )
            }
        }

        throw NseApiException(
            "India VIX was not found in NSE response"
        )
    }

    /*
     * ---------------------------------------------------------
     * NUMBER PARSER
     * ---------------------------------------------------------
     *
     * Some NSE responses may contain numbers as strings.
     * This handles both:
     *
     * 123.45
     *
     * and
     *
     * "123.45"
     * ---------------------------------------------------------
     */

    private fun getDouble(
        objectValue: JSONObject,
        key: String
    ): Double {

        val value =
            objectValue.opt(key)

        return when (value) {

            is Number ->
                value.toDouble()

            is String ->
                value
                    .replace(",", "")
                    .trim()
                    .toDoubleOrNull()
                    ?: 0.0

            else ->
                0.0
        }
    }

    /*
     * ---------------------------------------------------------
     * URL ENCODING
     * ---------------------------------------------------------
     */

    private fun encode(
        value: String
    ): String {

        return URLEncoder.encode(
            value,
            "UTF-8"
        )
    }

    /*
     * ---------------------------------------------------------
     * CURRENT TIME
     * ---------------------------------------------------------
     */

    private fun currentTime(): String {

        return SimpleDateFormat(
            "dd-MMM-yyyy HH:mm:ss",
            Locale.US
        ).format(
            Date()
        )
    }
}

/*
 * -------------------------------------------------------------
 * NSE API EXCEPTION
 * -------------------------------------------------------------
 */

class NseApiException(
    message: String
) : RuntimeException(message)
