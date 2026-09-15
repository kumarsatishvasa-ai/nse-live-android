package com.nselive.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**

* UI state for the MCX option-chain screen.
*
* Keep this class ONLY in McxViewModel.kt.
* Do not declare another McxUiState in MainActivity.kt.
  */
  data class McxUiState(
  val symbol: String = "CRUDEOIL",
  val expiries: List<String> = emptyList(),
  val selectedExpiry: String? = null,
  val chain: OptionChain? = null,
  val metrics: NseMetrics? = null,
  val loading: Boolean = false,
  val error: String? = null
  )

/**

* ViewModel for MCX option-chain data.
*
* Responsibilities:
* * Load available MCX expiries
* * Select an expiry
* * Load the option chain
* * Calculate metrics using MetricsCalculator
* * Expose everything through StateFlow
    */
    class McxViewModel(
    private val repository: McxRepository = McxRepository()
    ) : ViewModel() {

  private val _uiState = MutableStateFlow(McxUiState())

  val uiState: StateFlow<McxUiState> =
  _uiState.asStateFlow()

  init {
  loadExpiries()
  }

  /**

  * Change the MCX symbol.
  *
  * Example:
  * CRUDEOIL
  * GOLD
  * SILVER
  * NATURALGAS
    */
    fun setSymbol(symbol: String) {
    val normalized = symbol.trim().uppercase()

    if (normalized.isEmpty()) {
    return
    }

    _uiState.value = _uiState.value.copy(
    symbol = normalized,
    expiries = emptyList(),
    selectedExpiry = null,
    chain = null,
    metrics = null,
    error = null
    )

    loadExpiries()
    }

  /**

  * Load available expiry dates for the current symbol.
    */
    fun loadExpiries() {
    val symbol = _uiState.value.symbol

    viewModelScope.launch {
    _uiState.value = _uiState.value.copy(
    loading = true,
    error = null
    )

    ```
     try {
         val expiries = repository.getExpiries(symbol)

         if (expiries.isEmpty()) {
             _uiState.value = _uiState.value.copy(
                 expiries = emptyList(),
                 selectedExpiry = null,
                 loading = false,
                 error = "No MCX expiries found for $symbol."
             )
             return@launch
         }

         val currentSelected =
             _uiState.value.selectedExpiry

         val selected =
             if (
                 currentSelected != null &&
                 expiries.contains(currentSelected)
             ) {
                 currentSelected
             } else {
                 expiries.first()
             }

         _uiState.value = _uiState.value.copy(
             expiries = expiries,
             selectedExpiry = selected,
             loading = false,
             error = null
         )

         loadOptionChain(selected)

     } catch (exception: Exception) {

         _uiState.value = _uiState.value.copy(
             loading = false,
             error = exception.message
                 ?: "Unable to load MCX expiry dates."
         )
     }
    ```

    }
    }

  /**

  * Select an expiry date and load its option chain.
    */
    fun selectExpiry(expiry: String) {

    val normalizedExpiry = expiry.trim()

    if (normalizedExpiry.isEmpty()) {
    return
    }

    _uiState.value = _uiState.value.copy(
    selectedExpiry = normalizedExpiry,
    error = null
    )

    loadOptionChain(normalizedExpiry)
    }

  /**

  * Load option-chain data for the currently selected symbol
  * and expiry.
    */
    fun loadOptionChain(
    expiry: String? = _uiState.value.selectedExpiry
    ) {

    val state = _uiState.value
    val symbol = state.symbol
    val selectedExpiry = expiry

    if (selectedExpiry.isNullOrBlank()) {
    _uiState.value = state.copy(
    loading = false,
    error = "Please select an MCX expiry."
    )
    return
    }

    viewModelScope.launch {

    ```
     _uiState.value = _uiState.value.copy(
         selectedExpiry = selectedExpiry,
         loading = true,
         error = null
     )

     try {

         val chain =
             repository.getOptionChain(
                 symbol = symbol,
                 expiry = selectedExpiry
             )

         /*
          * MCX does not provide India VIX.
          *
          * Therefore MetricsCalculator receives null.
          * Other metrics such as PCR, Max Pain, Gamma Flip,
          * Call Wall and Put Wall can still be calculated.
          */
         val metrics =
             MetricsCalculator.calculate(
                 chain = chain,
                 vix = null
             )

         _uiState.value = _uiState.value.copy(
             chain = chain,
             metrics = metrics,
             loading = false,
             error = null
         )

     } catch (exception: Exception) {

         _uiState.value = _uiState.value.copy(
             loading = false,
             error = exception.message
                 ?: "Unable to load MCX option-chain data."
         )
     }
    ```

    }
    }

  /**

  * Refresh the current MCX option chain.
    */
    fun refresh() {

    val expiry =
    _uiState.value.selectedExpiry

    if (expiry.isNullOrBlank()) {
    loadExpiries()
    } else {
    loadOptionChain(expiry)
    }
    }

  /**

  * Clear the current error message.
    */
    fun clearError() {
    _uiState.value = _uiState.value.copy(
    error = null
    )
    }

  /**

  * Retry the last operation.
    */
    fun retry() {
    val state = _uiState.value

    if (state.selectedExpiry.isNullOrBlank()) {
    loadExpiries()
    } else {
    loadOptionChain(state.selectedExpiry)
    }
    }
    }
