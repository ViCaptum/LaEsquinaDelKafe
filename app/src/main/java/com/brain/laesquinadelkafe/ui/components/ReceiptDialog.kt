package com.brain.laesquinadelkafe.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brain.laesquinadelkafe.data.model.OrderWithItems
import com.brain.laesquinadelkafe.ui.utils.DateUtils
import java.util.Locale

@Composable
fun ReceiptDialog(
    orderWithItems: OrderWithItems, 
    onDismiss: () -> Unit,
    onDecreaseItemQuantity: ((Int) -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detalle del Pedido") },
        text = {
            Column {
                Text("Cliente: ${orderWithItems.order.clientName}", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fecha: ${DateUtils.formatDate(orderWithItems.order.timestamp)}", style = MaterialTheme.typography.bodySmall)
                    Text("Hora: ${DateUtils.formatTime(orderWithItems.order.timestamp)}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(orderWithItems.items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${item.quantity}x ${item.productName}${if(item.isToTakeAway) " (Llevar)" else " (Taza)"}", modifier = Modifier.weight(1f))
                            Text("S/. ${String.format(Locale.getDefault(), "%.2f", item.quantity * item.pricePerUnit)}")
                            
                            if (onDecreaseItemQuantity != null && !item.isToTakeAway && item.quantity > 0 && (orderWithItems.order.status == "PENDIENTE" || orderWithItems.order.status == "ENVIADO")) {
                                IconButton(onClick = { onDecreaseItemQuantity(item.id) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Devolver Taza", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                if (orderWithItems.order.paidAmount > 0 && !orderWithItems.order.isPaid) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pagado parcial:")
                        Text("S/. ${String.format(Locale.getDefault(), "%.2f", orderWithItems.order.paidAmount)}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val remaining = orderWithItems.order.totalAmount - orderWithItems.order.paidAmount
                        Text("Resta:", color = MaterialTheme.colorScheme.error)
                        Text("S/. ${String.format(Locale.getDefault(), "%.2f", remaining)}", color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", fontWeight = FontWeight.Bold)
                    Text("S/. ${String.format(Locale.getDefault(), "%.2f", orderWithItems.order.totalAmount)}", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
