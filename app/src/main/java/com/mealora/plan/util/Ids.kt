package com.mealora.plan.util

import java.util.UUID

/** Local, offline ID generation. IDs are never derived from any remote source. */
object Ids {
    fun newId(): String = UUID.randomUUID().toString()
}
