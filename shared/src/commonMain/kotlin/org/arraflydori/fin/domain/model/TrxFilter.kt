package org.arraflydori.fin.domain.model

import kotlinx.datetime.YearMonth

data class TrxFilter(
    val month: YearMonth,
    val type: TrxType? = null,
    val categoryId: String? = null,
    val accountId: String? = null
)
