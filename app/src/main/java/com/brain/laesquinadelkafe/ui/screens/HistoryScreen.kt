package com.brain.laesquinadelkafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.brain.laesquinadelkafe.data.model.OrderWithItems
import com.brain.laesquinadelkafe.ui.components.OrderCard
import com.brain.laesquinadelkafe.ui.components.ReceiptDialog
import com.brain.laesquinadelkafe.viewmodel.OrderViewModel

@Composable
fun HistoryScreen(viewModel: OrderViewModel) {
    val history by viewModel.historyOrders.collectAsState()
    var selectedOrder by remember { mutableStateOf<OrderWithItems?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay historial de pedidos pagados")
            }
        } else {
            LazyColumn {
                items(history) { orderWithItems ->
                    OrderCard(
                        orderWithItems = orderWithItems,
                        onCardClick = { selectedOrder = orderWithItems },
                        showActions = false
                    )
                }
            }
        }
    }

    selectedOrder?.let {
        ReceiptDialog(orderWithItems = it, onDismiss = { selectedOrder = null })
    }
}
