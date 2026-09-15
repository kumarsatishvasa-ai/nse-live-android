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

/**
 * MCX public option-chain client.
 *
 * This client is designed for the MCX public website/API.
 *
 * No Dhan account.
 * No broker API.
 * No Nubra market data.
 *
 * Expected MCX endpoint:
 *
 * GET https://www.mcxindia.com/GetOptionChain
 *
 * Query parameters:
 * InstrumentType=optfut
 * Symbol=CRUDEOIL
 * Expiry=17aug2026
 *
 * The response is converted into the application's common
 * OptionChain model so PCR / Max Pain / Gamma Flip /
 * Call Wall / Put Wall can use the same analytics pipeline.
 */
class McxApi {

    companion object {

        private const val BASE_URL =
            "https://www.mcxindia.com"

        private const val OPTION_CHAIN_URL =
            "$BASE_URL/GetOptionChain"

        private const val OPTION_CHAIN_PAGE =
            "$BASE_URL/market-data/option-chain"

        private const val INSTRUMENT_TYPE =
            "optfut"

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0 Safari/537.36"

        private val client =
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
    }

    /**
     * Public result returned by getOptionChain().
     */
    data class Result(
        val chain: OptionChain,
        val rawResponse: String
    )

    /**
     * Get MCX expiry dates.
     *
     * MCX has changed its public endpoints over time, so expiry
     * discovery is intentionally tolerant.
     *
     * If the expiry endpoint isn't available, the application
     * can still request an explicitly supplied expiry.
     */
    suspend fun getExpiries(
        symbol: String
    ): List<String> = withContext(Dispatchers.IO) {

        val normalizedSymbol =
            symbol.trim().uppercase(Locale.US)

        val candidateUrls = listOf(
            "$BASE_URL/GetExpiryDate" +
                    "?InstrumentType=$INSTRUMENT_TYPE" +
                    "&Symbol=$normalizedSymbol",

            "$BASE_URL/GetExpiry" +
                    "?InstrumentType=$INSTRUMENT_TYPE" +
                    "&Symbol=$normalizedSymbol",

            "$BASE_URL/GetExpiryDates" +
                    "?InstrumentType=$INSTRUMENT_TYPE" +
                    "&Symbol=$normalizedSymbol",

            "$BASE_URL/GetExpiryList" +
                    "?InstrumentType=$INSTRUMENT_TYPE" +
                    "&Symbol=$normalizedSymbol"
        )

        for (url in candidateUrls) {

            try {

                val body =
                    executeGet(url)

                val dates =
                    parseExpiryResponse(body)

                if (dates.isNotEmpty()) {
                    return@withContext dates
                }

            } catch (_: Exception) {
                // Try the next public endpoint.
            }
        }

        emptyList()
    }

    /**
     * Fetch MCX option-chain data.
     *
     * Example:
     *
     * getOptionChain(
     *     symbol = "CRUDEOIL",
     *     expiry = "17aug2026"
     * )
     */
    suspend fun getOptionChain(
        symbol: String,
        expiry: String
    ): Result = withContext(Dispatchers.IO) {

        val normalizedSymbol =
            symbol.trim().uppercase(Locale.US)

        val formattedExpiry =
            formatExpiryForMcx(expiry)

        val url =
            OPTION_CHAIN_URL +
                    "?InstrumentType=$INSTRUMENT_TYPE" +
                    "&Symbol=$normalizedSymbol" +
                    "&Expiry=$formattedExpiry"

        val body =
            executeGet(url)

        val chain =
            parseOptionChain(
                symbol = normalizedSymbol,
                expiry = expiry,
                response = body
            )

        Result(
            chain = chain,
            rawResponse = body
        )
    }

    /**
     * Converts:
     *
     * 17-Aug-2026
     * 17-AUG-2026
     * 17/08/2026
     * 2026-08-17
     * 17aug2026
     *
     * into:
     *
     * 17aug2026
     */
    fun formatExpiryForMcx(
        expiry: String
    ): String {

        val value =
            expiry
                .trim()
                .replace("\"", "")

        val formats =
            listOf(
                "dd-MMM-yyyy",
                "dd-MMM-yy",
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "dd/MMM/yyyy",
                "ddMMMyyyy"
            )

        for (format in formats) {

            try {

                val sdf =
                    SimpleDateFormat(
                        format,
                        Locale.US
                    )

                sdf.isLenient = false

                val date =
                    sdf.parse(value)

                if (date != null) {

                    return SimpleDateFormat(
                        "ddMMMyyyy",
                        Locale.US
                    )
                        .format(date)
                        .lowercase(Locale.US)
                }

            } catch (_: Exception) {
                // Try next format.
            }
        }

        return value
            .replace(" ", "")
            .replace("-", "")
            .lowercase(Locale.US)
    }

    /**
     * Execute a public MCX GET request.
     *
     * The browser page is requested first because the public
     * MCX service may expect normal browser cookies.
     */
    private fun executeGet(
        url: String
    ): String {

        try {

            val pageRequest =
                Request.Builder()
                    .url(OPTION_CHAIN_PAGE)
                    .get()
                    .header(
                        "User-Agent",
                        USER_AGENT
                    )
                    .header(
                        "Accept",
                        "text/html,application/xhtml+xml"
                    )
                    .build()

            client
                .newCall(pageRequest)
                .execute()
                .use {
                    // Intentionally only prime cookies/session.
                }

        } catch (_: Exception) {
            // Continue to API request.
        }

        val request =
            Request.Builder()
                .url(url)
                .get()
                .header(
                    "User-Agent",
                    USER_AGENT
                )
                .header(
                    "Accept",
                    "application/json, text/plain, */*"
                )
                .header(
                    "Accept-Language",
                    "en-US,en;q=0.9"
                )
                .header(
                    "Cache-Control",
                    "no-cache"
                )
                .header(
                    "Pragma",
                    "no-cache"
                )
                .header(
                    "Referer",
                    OPTION_CHAIN_PAGE
                )
                .header(
                    "X-Requested-With",
                    "XMLHttpRequest"
                )
                .build()

        client
            .newCall(request)
            .execute()
            .use { response ->

                val body =
                    response.body?.string()
                        ?: ""

                if (!response.isSuccessful) {

                    throw McxApiException(
                        "MCX HTTP ${response.code}: " +
                                body.take(500)
                    )
                }

                if (body.isBlank()) {

                    throw McxApiException(
                        "MCX returned an empty response."
                    )
                }

                return body
            }
    }

    /**
     * Parses MCX expiry responses.
     */
    private fun parseExpiryResponse(
        response: String
    ): List<String> {

        val output =
            mutableListOf<String>()

        val root =
            unwrapResponse(response)

        when (root) {

            is JSONArray -> {

                for (i in 0 until root.length()) {

                    val item =
                        root.opt(i)

                    when (item) {

                        is JSONObject -> {

                            val date =
                                firstString(
                                    item,
                                    listOf(
                                        "Expiry",
                                        "ExpiryDate",
                                        "expiry",
                                        "expiryDate",
                                        "EXPIRY",
                                        "EXPIRYDATE"
                                    )
                                )

                            if (!date.isNullOrBlank()) {
                                output.add(date)
                            }
                        }

                        is String -> {
                            output.add(item)
                        }
                    }
                }
            }

            is JSONObject -> {

                val possibleArrays =
                    listOf(
                        "Data",
                        "data",
                        "ExpiryDates",
                        "expiryDates",
                        "Table",
                        "table",
                        "Result",
                        "result"
                    )

                for (key in possibleArrays) {

                    val array =
                        root.optJSONArray(key)

                    if (array != null) {

                        for (i in 0 until array.length()) {

                            val item =
                                array.opt(i)

                            when (item) {

                                is JSONObject -> {

                                    val date =
                                        firstString(
                                            item,
                                            listOf(
                                                "Expiry",
                                                "ExpiryDate",
                                                "expiry",
                                                "expiryDate",
                                                "EXPIRY",
                                                "EXPIRYDATE"
                                            )
                                        )

                                    if (!date.isNullOrBlank()) {
                                        output.add(date)
                                    }
                                }

                                is String -> {
                                    output.add(item)
                                }
                            }
                        }
                    }
                }
            }
        }

        return output
            .map {
                it.trim()
            }
            .filter {
                it.isNotEmpty()
            }
            .distinct()
    }

    /**
     * Parse the confirmed MCX response shape.
     *
     * Expected:
     *
     * {
     *   "Summary": {
     *      "AsOn": "/Date(....)/",
     *      "Count": 10
     *   },
     *   "Data": [
     *      {
     *        "CE_StrikePrice": ...,
     *        "CE_OpenInterest": ...,
     *        "CE_ChangeInOI": ...,
     *        "CE_Volume": ...,
     *        "CE_LTP": ...,
     *        "CE_BidPrice": ...,
     *        "CE_AskPrice": ...,
     *
     *        "PE_StrikePrice": ...,
     *        "PE_OpenInterest": ...,
     *        "PE_ChangeInOI": ...,
     *        "PE_Volume": ...,
     *        "PE_LTP": ...,
     *        "PE_BidPrice": ...,
     *        "PE_AskPrice": ...,
     *
     *        "UnderlyingValue": ...
     *      }
     *   ]
     * }
     */
    private fun parseOptionChain(
        symbol: String,
        expiry: String,
        response: String
    ): OptionChain {

        val root =
            unwrapResponse(response)

        if (root !is JSONObject) {

            throw McxApiException(
                "Unexpected MCX response format."
            )
        }

        val rows =
            findDataArray(root)

        if (rows == null || rows.length() == 0) {

            throw McxApiException(
                "MCX returned no option-chain rows."
            )
        }

        val contracts =
            mutableListOf<OptionContract>()

        var underlyingValue = 0.0

        for (i in 0 until rows.length()) {

            val row =
                rows.optJSONObject(i)
                    ?: continue

            val strike =
                firstDouble(
                    row,
                    listOf(
                        "CE_StrikePrice",
                        "PE_StrikePrice",
                        "StrikePrice",
                        "strikePrice",
                        "Strike"
                    )
                )

            if (strike <= 0.0) {
                continue
            }

            if (underlyingValue <= 0.0) {

                underlyingValue =
                    firstDouble(
                        row,
                        listOf(
                            "UnderlyingValue",
                            "Underlying",
                            "underlyingValue",
                            "Spot",
                            "spot"
                        )
                    )
            }

            val callOi =
                firstDouble(
                    row,
                    listOf(
                        "CE_OpenInterest",
                        "CE_OI",
                        "CallOpenInterest",
                        "CallOI"
                    )
                )

            val putOi =
                firstDouble(
                    row,
                    listOf(
                        "PE_OpenInterest",
                        "PE_OI",
                        "PutOpenInterest",
                        "PutOI"
                    )
                )

            val callVolume =
                firstDouble(
                    row,
                    listOf(
                        "CE_Volume",
                        "CE_Vol",
                        "CallVolume"
                    )
                )

            val putVolume =
                firstDouble(
                    row,
                    listOf(
                        "PE_Volume",
                        "PE_Vol",
                        "PutVolume"
                    )
                )

            val callLtp =
                firstDouble(
                    row,
                    listOf(
                        "CE_LTP",
                        "CE_LastTradedPrice",
                        "CE_LastPrice",
                        "CallLTP"
                    )
                )

            val putLtp =
                firstDouble(
                    row,
                    listOf(
                        "PE_LTP",
                        "PE_LastTradedPrice",
                        "PE_LastPrice",
                        "PutLTP"
                    )
                )

            val callChangeOi =
                firstDouble(
                    row,
                    listOf(
                        "CE_ChangeInOI",
                        "CE_ChangeOI",
                        "CallChangeInOI"
                    )
                )

            val putChangeOi =
                firstDouble(
                    row,
                    listOf(
                        "PE_ChangeInOI",
                        "PE_ChangeOI",
                        "PutChangeInOI"
                    )
                )

            contracts.add(
                OptionContract(
                    strikePrice = strike,

                    callOi = callOi,
                    putOi = putOi,

                    callVolume = callVolume,
                    putVolume = putVolume,

                    callLtp = callLtp,
                    putLtp = putLtp,

                    callOiChange = callChangeOi,
                    putOiChange = putChangeOi
                )
            )
        }

        if (contracts.isEmpty()) {

            throw McxApiException(
                "MCX response contained rows but no valid strikes."
            )
        }

        val timestamp =
            extractTimestamp(root)

        return OptionChain(
            underlyingValue = underlyingValue,
            timestamp = timestamp,
            contracts = contracts
        )
    }

    /**
     * Finds the Data array in the MCX response.
     */
    private fun findDataArray(
        root: JSONObject
    ): JSONArray? {

        val names =
            listOf(
                "Data",
                "data",
                "Table",
                "table",
                "Result",
                "result"
            )

        for (name in names) {

            val array =
                root.optJSONArray(name)

            if (array != null) {
                return array
            }
        }

        return null
    }

    /**
     * Handles ASP.NET WebMethod responses:
     *
     * {
     *   "d": "{\"Summary\":...,\"Data\":[...]}"
     * }
     *
     * and normal JSON responses.
     */
    private fun unwrapResponse(
        response: String
    ): Any {

        val first =
            JSONObject(response)

        val d =
            first.opt("d")

        if (d is String) {

            val trimmed =
                d.trim()

            if (
                trimmed.startsWith("{") ||
                trimmed.startsWith("[")
            ) {

                return if (
                    trimmed.startsWith("{")
                ) {
                    JSONObject(trimmed)
                } else {
                    JSONArray(trimmed)
                }
            }

            return first
        }

        return first
    }

    /**
     * Extract timestamp from MCX Summary.AsOn.
     */
    private fun extractTimestamp(
        root: JSONObject
    ): String {

        val summary =
            root.optJSONObject("Summary")

        val asOn =
            summary?.optString("AsOn")
                ?: root.optString("AsOn")

        if (asOn.isBlank()) {

            return currentTimestamp()
        }

        return parseDotNetDate(asOn)
    }

    /**
     * Converts:
     *
     * /Date(1784916000000)/
     *
     * to:
     *
     * dd-MMM-yyyy HH:mm:ss
     */
    private fun parseDotNetDate(
        value: String
    ): String {

        val regex =
            Regex("""/Date\((-?\d+)(?:[+-]\d+)?\)/""")

        val match =
            regex.find(value)

        if (match == null) {
            return value
        }

        return try {

            val millis =
                match.groupValues[1]
                    .toLong()

            SimpleDateFormat(
                "dd-MMM-yyyy HH:mm:ss",
                Locale.US
            )
                .format(Date(millis))

        } catch (_: Exception) {

            currentTimestamp()
        }
    }

    private fun currentTimestamp(): String {

        return SimpleDateFormat(
            "dd-MMM-yyyy HH:mm:ss",
            Locale.US
        )
            .format(Date())
    }

    /**
     * Read the first usable String from several possible keys.
     */
    private fun firstString(
        obj: JSONObject,
        keys: List<String>
    ): String? {

        for (key in keys) {

            if (!obj.has(key)) {
                continue
            }

            val value =
                obj.opt(key)

            if (
                value != null &&
                value != JSONObject.NULL
            ) {

                val text =
                    value.toString().trim()

                if (text.isNotEmpty()) {
                    return text
                }
            }
        }

        return null
    }

    /**
     * Read the first usable numeric value.
     */
    private fun firstDouble(
        obj: JSONObject,
        keys: List<String>
    ): Double {

        for (key in keys) {

            if (!obj.has(key)) {
                continue
            }

            val value =
                obj.opt(key)

            val number =
                parseDouble(value)

            if (number != null) {
                return number
            }
        }

        return 0.0
    }

    /**
     * MCX sometimes returns:
     *
     * ""
     * null
     * "1,234"
     * "1234.50"
     * numbers
     */
    private fun parseDouble(
        value: Any?
    ): Double? {

        if (
            value == null ||
            value == JSONObject.NULL
        ) {
            return null
        }

        if (value is Number) {
            return value.toDouble()
        }

        val text =
            value
                .toString()
                .trim()
                .replace(",", "")

        if (text.isEmpty()) {
            return null
        }

        return text.toDoubleOrNull()
    }

    class McxApiException(
        message: String
    ) : RuntimeException(message)
}
