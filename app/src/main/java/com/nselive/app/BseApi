package com.nselive.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class BseApi {

    companion object {

        private const val BASE_URL =
            "https://www.bseindia.com"

        private const val API_URL =
            "https://api.bseindia.com"

        /*
         * Confirmed BSE endpoint.
         *
         * Returns:
         * - expiry dates
         * - strikes
         * - underlying value
         */
        private const val EXPIRY_ENDPOINT =
            "/BseIndiaAPI/api/ddlExpiry_IV/w"

        /*
         * Confirmed BSE option-chain endpoint.
         */
        private const val OPTION_CHAIN_ENDPOINT =
            "/BseIndiaAPI/api/DerivOptionChain_IV/w"

        /*
         * Confirmed scrip codes from your Python code.
         */
        private val SCRIP_CODES =
            mapOf(
                "SENSEX" to "1",
                "BANKEX" to "12",
                "BSEFOCUSEDIT" to "75"
            )
    }

    /*
     * ---------------------------------------------------------
     * HTTP CLIENT
     * ---------------------------------------------------------
     */

    private val client =
        OkHttpClient.Builder()

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
     * HEADERS
     * ---------------------------------------------------------
     */

    private val headers =
        mapOf(

            "Accept" to
                    "application/json, text/plain, */*",

            "Accept-Language" to
                    "en-US,en;q=0.9",

            "Accept-Encoding" to
                    "gzip, deflate, br",

            "Cache-Control" to
                    "no-cache",

            "Pragma" to
                    "no-cache",

            "X-Requested-With" to
                    "XMLHttpRequest",

            "Sec-Fetch-Dest" to
                    "empty",

            "Sec-Fetch-Mode" to
                    "cors",

            "Sec-Fetch-Site" to
                    "same-site",

            "Sec-CH-UA" to
                    "\"Google Chrome\";v=\"124\", " +
                    "\"Chromium\";v=\"124\", " +
                    "\"Not-A.Brand\";v=\"99\"",

            "Sec-CH-UA-Mobile" to
                    "?0",

            "Sec-CH-UA-Platform" to
                    "\"Windows\"",

            "User-Agent" to
                    "Mozilla/5.0 " +
                    "(Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) " +
                    "Chrome/124.0 Safari/537.36",

            "Referer" to
                    "$BASE_URL/",

            "Origin" to
                    BASE_URL
        )

    /*
     * ---------------------------------------------------------
     * PRIME BSE SESSION
     * ---------------------------------------------------------
     */

    private suspend fun primeBse() {

        withContext(Dispatchers.IO) {

            try {

                val request =
                    Request.Builder()
                        .url(BASE_URL + "/")
                        .get()
                        .header(
                            "User-Agent",
                            headers["User-Agent"]!!
                        )
                        .build()

                client
                    .newCall(request)
                    .execute()
                    .close()

            } catch (_: Exception) {

                /*
                 * Do not fail here.
                 *
                 * The actual API request will determine
                 * whether BSE is reachable.
                 */
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * BASIC GET
     * ---------------------------------------------------------
     */

    private suspend fun get(
        url: String
    ): String =
        withContext(Dispatchers.IO) {

            val builder =
                Request.Builder()
                    .url(url)
                    .get()

            headers.forEach { (key, value) ->

                builder.header(
                    key,
                    value
                )
            }

            val request =
                builder.build()

            client
                .newCall(request)
                .execute()
                .use { response ->

                    val body =
                        response.body
                            ?.string()
                            ?: ""

                    if (!response.isSuccessful) {

                        throw BseApiException(
                            "BSE HTTP ${response.code}: " +
                                    body.take(500)
                        )
                    }

                    if (body.isBlank()) {

                        throw BseApiException(
                            "BSE returned an empty response"
                        )
                    }

                    body
                }
        }

    /*
     * ---------------------------------------------------------
     * GET SCRIP CODE
     * ---------------------------------------------------------
     */

    private fun getScripCode(
        symbol: String
    ): String {

        return SCRIP_CODES[
            symbol
                .trim()
                .uppercase(Locale.US)
        ]
            ?: throw BseApiException(
                "No BSE scrip code configured for $symbol"
            )
    }

    /*
     * ---------------------------------------------------------
     * REFERENCE DATA
     * ---------------------------------------------------------
     *
     * BSE response:
     *
     * {
     *   "Table1": [...expiry dates...],
     *   "Table": [...strikes...],
     *   "Table2": [
     *      {
     *          "UlaValue": ...
     *      }
     *   ]
     * }
     */

    suspend fun getReferenceData(
        symbol: String
    ): BseReferenceData {

        primeBse()

        val scripCode =
            getScripCode(symbol)

        val url =
            API_URL +
                    EXPIRY_ENDPOINT +
                    "?ProductType=IO" +
                    "&scrip_cd=$scripCode" +
                    "&_=${System.currentTimeMillis()}"

        val json =
            get(url)

        return parseReferenceData(
            json = json,
            symbol = symbol
        )
    }

    /*
     * ---------------------------------------------------------
     * EXPIRIES
     * ---------------------------------------------------------
     */

    suspend fun getExpiries(
        symbol: String
    ): List<String> {

        val reference =
            getReferenceData(
                symbol
            )

        if (
            reference.expiries.isEmpty()
        ) {

            throw BseApiException(
                "BSE returned no expiry dates for $symbol"
            )
        }

        return reference.expiries
    }

    /*
     * ---------------------------------------------------------
     * OPTION CHAIN
     * ---------------------------------------------------------
     *
     * This is the direct conversion of your Python
     * get_option_chain().
     * ---------------------------------------------------------
     */

    suspend fun getOptionChain(
        symbol: String,
        expiry: String
    ): BseOptionChain {

        primeBse()

        val scripCode =
            getScripCode(symbol)

        if (expiry.isBlank()) {

            throw BseApiException(
                "BSE expiry cannot be empty"
            )
        }

        val url =
            API_URL +
                    OPTION_CHAIN_ENDPOINT +
                    "?Expiry=${encodeUrl(expiry)}" +
                    "&scrip_cd=$scripCode" +
                    "&strprice=0" +
                    "&_=${System.currentTimeMillis()}"

        val json =
            get(url)

        return parseOptionChain(
            json = json,
            symbol = symbol,
            expiry = expiry
        )
    }

    /*
     * ---------------------------------------------------------
     * PARSE REFERENCE DATA
     * ---------------------------------------------------------
     */

    private fun parseReferenceData(
        json: String,
        symbol: String
    ): BseReferenceData {

        val root =
            parseJsonObject(
                json,
                "BSE reference response"
            )

        /*
         * -----------------------------------------------------
         * TABLE 1
         * -----------------------------------------------------
         */

        val expiries =
            mutableListOf<String>()

        val table1 =
            root.optJSONArray(
                "Table1"
            )

        if (table1 != null) {

            for (
                i in 0 until table1.length()
            ) {

                val row =
                    table1.optJSONObject(i)
                        ?: continue

                val expiry =
                    row.optString(
                        "ExpiryDate",
                        ""
                    ).trim()

                if (
                    expiry.isNotEmpty() &&
                    !expiries.contains(expiry)
                ) {

                    expiries.add(
                        expiry
                    )
                }
            }
        }

        /*
         * -----------------------------------------------------
         * TABLE
         * -----------------------------------------------------
         *
         * Contains valid strikes.
         */

        val strikes =
            mutableListOf<Double>()

        val table =
            root.optJSONArray(
                "Table"
            )

        if (table != null) {

            for (
                i in 0 until table.length()
            ) {

                val row =
                    table.optJSONObject(i)
                        ?: continue

                val strike =
                    getDouble(
                        row,
                        "Strike_Price1"
                    )

                if (
                    strike != null &&
                    strike > 0.0
                ) {

                    strikes.add(
                        strike
                    )
                }
            }
        }

        /*
         * -----------------------------------------------------
         * TABLE 2
         * -----------------------------------------------------
         *
         * UlaValue = underlying value.
         */

        var spot = 0.0

        val table2 =
            root.optJSONArray(
                "Table2"
            )

        if (
            table2 != null &&
            table2.length() > 0
        ) {

            val first =
                table2.optJSONObject(0)

            if (first != null) {

                spot =
                    getDouble(
                        first,
                        "UlaValue"
                    ) ?: 0.0
            }
        }

        return BseReferenceData(

            symbol =
                symbol,

            expiries =
                expiries,

            strikes =
                strikes
                    .distinct()
                    .sorted(),

            underlyingValue =
                spot
        )
    }

    /*
     * ---------------------------------------------------------
     * PARSE OPTION CHAIN
     * ---------------------------------------------------------
     */

    private fun parseOptionChain(
        json: String,
        symbol: String,
        expiry: String
    ): BseOptionChain {

        val root =
            parseJsonObject(
                json,
                "BSE option-chain response"
            )

        /*
         * -----------------------------------------------------
         * TABLE
         * -----------------------------------------------------
         */

        val rows =
            root.optJSONArray(
                "Table"
            )
                ?: throw BseApiException(
                    "BSE option-chain response has no Table"
                )

        if (rows.length() == 0) {

            throw BseApiException(
                "BSE returned no option-chain rows for " +
                        "$symbol $expiry"
            )
        }

        val contracts =
            mutableListOf<BseOptionContract>()

        var spot = 0.0

        /*
         * -----------------------------------------------------
         * EACH STRIKE
         * -----------------------------------------------------
         */

        for (
            i in 0 until rows.length()
        ) {

            val row =
                rows.optJSONObject(i)
                    ?: continue

            /*
             * Strike_Price1
             *
             * BSE supplies this as a numeric string.
             */

            val strike =
                getDouble(
                    row,
                    "Strike_Price1"
                )
                    ?: continue

            if (strike <= 0.0) {
                continue
            }

            /*
             * UlaValue is repeated on every row.
             *
             * Only need to capture it once.
             */

            if (spot <= 0.0) {

                spot =
                    getDouble(
                        row,
                        "UlaValue"
                    ) ?: 0.0
            }

            /*
             * =================================================
             * CALL
             * =================================================
             *
             * Your Python code confirmed:
             *
             * C_Open_Interest
             * C_Absolute_Change_OI
             * C_Vol_Traded
             * C_IV
             * C_Last_Trd_Price
             * C_BidPrice
             * C_OfferPrice
             */

            val callOi =
                getDouble(
                    row,
                    "C_Open_Interest"
                ) ?: 0.0

            val callOiChange =
                getDouble(
                    row,
                    "C_Absolute_Change_OI"
                ) ?: 0.0

            val callVolume =
                getDouble(
                    row,
                    "C_Vol_Traded"
                ) ?: 0.0

            val callIv =
                getDouble(
                    row,
                    "C_IV"
                ) ?: 0.0

            val callLtp =
                getDouble(
                    row,
                    "C_Last_Trd_Price"
                ) ?: 0.0

            val callBid =
                getDouble(
                    row,
                    "C_BidPrice"
                ) ?: 0.0

            val callAsk =
                getDouble(
                    row,
                    "C_OfferPrice"
                ) ?: 0.0

            /*
             * =================================================
             * PUT
             * =================================================
             *
             * Unprefixed fields are PUT.
             */

            val putOi =
                getDouble(
                    row,
                    "Open_Interest"
                ) ?: 0.0

            val putOiChange =
                getDouble(
                    row,
                    "Absolute_Change_OI"
                ) ?: 0.0

            val putVolume =
                getDouble(
                    row,
                    "Vol_Traded"
                ) ?: 0.0

            val putIv =
                getDouble(
                    row,
                    "IV"
                ) ?: 0.0

            val putLtp =
                getDouble(
                    row,
                    "Last_Trd_Price"
                ) ?: 0.0

            val putBid =
                getDouble(
                    row,
                    "BidPrice"
                ) ?: 0.0

            val putAsk =
                getDouble(
                    row,
                    "OfferPrice"
                ) ?: 0.0

            /*
             * Add contract.
             */

            contracts.add(

                BseOptionContract(

                    symbol =
                        symbol,

                    expiry =
                        expiry,

                    strike =
                        strike,

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

                    callBid =
                        callBid,

                    callAsk =
                        callAsk,

                    putOi =
                        putOi,

                    putOiChange =
                        putOiChange,

                    putVolume =
                        putVolume,

                    putIv =
                        putIv,

                    putLtp =
                        putLtp,

                    putBid =
                        putBid,

                    putAsk =
                        putAsk
                )
            )
        }

        if (contracts.isEmpty()) {

            throw BseApiException(
                "BSE returned rows but no valid strikes"
            )
        }

        /*
         * -----------------------------------------------------
         * TIMESTAMP
         * -----------------------------------------------------
         */

        var timestamp =
            currentTime()

        val ason =
            root.optJSONObject(
                "ASON"
            )

        if (ason != null) {

            val dt =
                ason.optString(
                    "DT_TM",
                    ""
                ).trim()

            if (dt.isNotEmpty()) {

                timestamp = dt
            }
        }

        return BseOptionChain(

            exchange =
                "BSE",

            symbol =
                symbol,

            underlyingValue =
                spot,

            expiry =
                expiry,

            timestamp =
                timestamp,

            contracts =
                contracts
        )
    }

    /*
     * ---------------------------------------------------------
     * JSON OBJECT
     * ---------------------------------------------------------
     */

    private fun parseJsonObject(
        json: String,
        description: String
    ): JSONObject {

        return try {

            JSONObject(json)

        } catch (e: Exception) {

            throw BseApiException(
                "$description is not valid JSON: " +
                        json.take(500)
            )
        }
    }

    /*
     * ---------------------------------------------------------
     * NUMBER PARSER
     * ---------------------------------------------------------
     *
     * Important because BSE can return:
     *
     * ""
     * "1200"
     * "1,200"
     * 1200
     * 1200.50
     */

    private fun getDouble(
        objectValue: JSONObject?,
        key: String
    ): Double? {

        if (objectValue == null) {
            return null
        }

        val value =
            objectValue.opt(key)

        if (
            value == null ||
            value == JSONObject.NULL
        ) {
            return null
        }

        if (value is Number) {

            return value.toDouble()
        }

        if (value is String) {

            val text =
                value
                    .replace(",", "")
                    .trim()

            if (text.isEmpty()) {
                return 0.0
            }

            return text.toDoubleOrNull()
        }

        return null
    }

    /*
     * ---------------------------------------------------------
     * URL ENCODING
     * ---------------------------------------------------------
     */

    private fun encodeUrl(
        value: String
    ): String {

        return java.net.URLEncoder
            .encode(
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
 * =============================================================
 * BSE REFERENCE DATA
 * =============================================================
 */

data class BseReferenceData(

    val symbol: String,

    val expiries: List<String>,

    val strikes: List<Double>,

    val underlyingValue: Double
)

/*
 * =============================================================
 * BSE OPTION CONTRACT
 * =============================================================
 */

data class BseOptionContract(

    val symbol: String,

    val expiry: String,

    val strike: Double,

    val callOi: Double = 0.0,

    val callOiChange: Double = 0.0,

    val callVolume: Double = 0.0,

    val callIv: Double = 0.0,

    val callLtp: Double = 0.0,

    val callBid: Double = 0.0,

    val callAsk: Double = 0.0,

    val putOi: Double = 0.0,

    val putOiChange: Double = 0.0,

    val putVolume: Double = 0.0,

    val putIv: Double = 0.0,

    val putLtp: Double = 0.0,

    val putBid: Double = 0.0,

    val putAsk: Double = 0.0
)

/*
 * =============================================================
 * BSE OPTION CHAIN
 * =============================================================
 */

data class BseOptionChain(

    val exchange: String,

    val symbol: String,

    val underlyingValue: Double,

    val expiry: String,

    val timestamp: String,

    val contracts: List<BseOptionContract>
)

/*
 * =============================================================
 * BSE EXCEPTION
 * =============================================================
 */

class BseApiException(
    message: String
) : RuntimeException(message)
