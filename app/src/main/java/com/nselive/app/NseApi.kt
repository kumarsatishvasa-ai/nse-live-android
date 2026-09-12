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
     * NSE uses cookies during normal browser requests.
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

                val now =
                    System.currentTimeMillis()

                return this@NseApi.cookies[url.host]
                    ?.filter { cookie ->
                        cookie.expiresAt > now
                    }
                    ?: emptyList()
            }
        }


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
            .build()


    /*
     * Browser-like headers.
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
                    "Mozilla/5.0 (Linux; Android 14) " +
                    "AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 " +
                    "Mobile Safari/537.36",

            "X-Requested-With" to
                    "XMLHttpRequest"
        )


    /*
     * Generic GET request.
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


            client
                .newCall(
                    builder.build()
                )
                .execute()
                .use { response ->

                    val body =
                        response.body?.string()
                            ?: ""

                    if (!response.isSuccessful) {

                        throw NseApiException(
                            "NSE HTTP ${response.code}: ${body.take(200)}"
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
     * Establish NSE cookies before API requests.
     */
    private suspend fun prime() {

        try {

            get(
                HOME_PAGE,
                HOME_PAGE
            )

            get(
                OPTION_CHAIN_PAGE,
                HOME_PAGE
            )

        } catch (e: Exception) {

            /*
             * Do not stop here.
             *
             * The actual API request will determine
             * whether NSE is accessible.
             */
        }
    }


    /*
     * Get NIFTY expiry dates.
     */
    suspend fun getExpiries(
        symbol: String = "NIFTY"
    ): List<String> {

        prime()

        val url =
            CONTRACT_INFO_URL +
                    "?symbol=${encode(symbol)}" +
                    "&_=${System.currentTimeMillis()}"


        val json =
            get(
                url,
                OPTION_CHAIN_PAGE
            )


        val root =
            JSONObject(json)


        val result =
            mutableListOf<String>()


        /*
         * Some NSE responses put expiryDates
         * at the root.
         */
        val rootExpiryDates =
            root.optJSONArray(
                "expiryDates"
            )


        if (rootExpiryDates != null) {

            for (
                i in
                0 until rootExpiryDates.length()
            ) {

                val expiry =
                    rootExpiryDates
                        .optString(i)
                        .trim()

                if (expiry.isNotEmpty()) {

                    result.add(expiry)
                }
            }
        }


        /*
         * Other responses put them inside records.
         */
        if (result.isEmpty()) {

            val records =
                root.optJSONObject(
                    "records"
                )


            val recordsExpiryDates =
                records?.optJSONArray(
                    "expiryDates"
                )


            if (recordsExpiryDates != null) {

                for (
                    i in
                    0 until recordsExpiryDates.length()
                ) {

                    val expiry =
                        recordsExpiryDates
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


    /*
     * Get NIFTY option chain.
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


    /*
     * Get India VIX.
     */
    suspend fun getIndiaVix(): IndiaVix {

        prime()


        val url =
            ALL_INDICES_URL +
                    "?_=${System.currentTimeMillis()}"


        val json =
            get(
                url,
                "$BASE_URL/market-data/live-market-indices"
            )


        val root =
            JSONObject(json)


        val data =
            root.optJSONArray("data")
                ?: throw NseApiException(
                    "NSE India VIX response has no data"
                )


        for (
            i in
            0 until data.length()
        ) {

            val row =
                data.optJSONObject(i)
                    ?: continue


            val index =
                row.optString(
                    "index",
                    ""
                )
                    .trim()
                    .uppercase(Locale.US)


            if (
                index == "INDIA VIX" ||
                index == "INDIA VIX "
            ) {

                val value =
                    row.optDouble(
                        "last",
                        Double.NaN
                    )


                if (value.isNaN()) {

                    throw NseApiException(
                        "India VIX value was not returned by NSE"
                    )
                }


                return IndiaVix(

                    value = value,

                    changePct =
                        row.optDouble(
                            "percentChange",
                            0.0
                        ),

                    open =
                        row.optDouble(
                            "open",
                            0.0
                        )
                )
            }
        }


        throw NseApiException(
            "India VIX was not found in NSE response"
        )
    }


    /*
     * Parse option-chain JSON.
     */
    private fun parseOptionChain(
        json: String
    ): OptionChain {

        val root =
            JSONObject(json)


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
            records.optDouble(
                "underlyingValue",
                0.0
            )


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
            i in
            0 until data.length()
        ) {

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
            )
        }


        if (contracts.isEmpty()) {

            throw NseApiException(
                "NSE returned no option contracts"
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


    /*
     * Current local timestamp.
     */
    private fun now(): String {

        return SimpleDateFormat(
            "dd-MMM-yyyy HH:mm:ss",
            Locale.US
        ).format(
            Date()
        )
    }
}


class NseApiException(
    message: String
) : RuntimeException(message)
