package com.crapp.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pushes today's bowel-movement count to a paired watch (docs/future-features.md
 * spec 5) via the Wearable Data Layer API's [com.google.android.gms.wearable.DataClient]
 * -- push rather than the watch polling on demand, since a `DataItem` update is
 * delivered once the watch is next reachable even if it was briefly out of BLE
 * range when this ran.
 */
object WearSyncPublisher {
    fun pushTodayCount(context: Context, count: Int) {
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        val request = PutDataMapRequest.create(WearProtocol.PATH_TODAY_COUNT).apply {
            dataMap.putInt(WearProtocol.KEY_COUNT, count)
            dataMap.putString(WearProtocol.KEY_DATE, today)
            // Forces a fresh DataItem even if the count is unchanged from last push
            // (e.g. a delete followed by a re-add landing back on the same number).
            dataMap.putLong("updatedAtEpochMillis", Instant.now().toEpochMilli())
        }
        Wearable.getDataClient(context).putDataItem(request.asPutDataRequest().setUrgent())
    }
}
