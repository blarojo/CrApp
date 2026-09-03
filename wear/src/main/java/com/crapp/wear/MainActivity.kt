package com.crapp.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import java.time.LocalDate

/**
 * Wear OS companion app (docs/future-features.md spec 5): the poo icon, today's
 * count, and a `+` to log one -- nothing else. Food/medication/energy/walk stay
 * phone-only per the original requirement. Standalone-installable (see the
 * manifest's `com.google.android.wearable.standalone` meta-data), but only useful
 * once paired with the phone app -- see `PhoneWearableListenerService` there.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WatchScreen()
            }
        }
    }
}

@Composable
private fun WatchScreen() {
    val context = LocalContext.current
    var todayCount by remember { mutableIntStateOf(0) }
    var sending by remember { mutableStateOf(false) }

    // Seeds the current count from whatever the phone last pushed, then keeps
    // listening for live updates -- covers both "already synced before this
    // launch" and "phone pushes a change while this screen is open."
    LaunchedEffect(Unit) {
        Wearable.getDataClient(context).dataItems.addOnSuccessListener { items ->
            for (i in 0 until items.count) {
                val item = items[i]
                if (item.uri.path == WearProtocol.PATH_TODAY_COUNT) {
                    todayCount = readCountIfToday(DataMapItem.fromDataItem(item).dataMap)
                }
            }
            items.release()
        }
    }

    DisposableEffect(Unit) {
        val dataClient = Wearable.getDataClient(context)
        val listener = DataClient.OnDataChangedListener { events: DataEventBuffer ->
            for (event in events) {
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearProtocol.PATH_TODAY_COUNT) {
                    todayCount = readCountIfToday(DataMapItem.fromDataItem(event.dataItem).dataMap)
                }
            }
        }
        dataClient.addListener(listener)
        onDispose { dataClient.removeListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text("💩", style = MaterialTheme.typography.display1)
            Text(todayCount.toString(), style = MaterialTheme.typography.display2)
            Text("today", style = MaterialTheme.typography.caption1)
            Button(
                enabled = !sending,
                onClick = {
                    sending = true
                    sendLogMovementMessage(context) { sending = false }
                }
            ) {
                Text("+")
            }
        }
    }
}

/** Ignores a stale count from a previous day -- the phone only pushes on change, so a day boundary alone wouldn't reset this screen otherwise. */
private fun readCountIfToday(dataMap: com.google.android.gms.wearable.DataMap): Int {
    val date = dataMap.getString(WearProtocol.KEY_DATE)
    return if (date == LocalDate.now().toString()) dataMap.getInt(WearProtocol.KEY_COUNT) else 0
}

private fun sendLogMovementMessage(context: android.content.Context, onFinished: () -> Unit) {
    val nodeClient = Wearable.getNodeClient(context)
    val messageClient = Wearable.getMessageClient(context)
    nodeClient.connectedNodes.addOnSuccessListener { nodes ->
        if (nodes.isEmpty()) {
            onFinished()
            return@addOnSuccessListener
        }
        var remaining = nodes.size
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, WearProtocol.PATH_LOG_MOVEMENT, ByteArray(0))
                .addOnCompleteListener {
                    remaining--
                    if (remaining <= 0) onFinished()
                }
        }
    }.addOnFailureListener { onFinished() }
}
