package com.brain.laesquinadelkafe.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.brain.laesquinadelkafe.data.model.OrderWithItems
import com.brain.laesquinadelkafe.ui.theme.SoftGreen
import com.brain.laesquinadelkafe.ui.theme.SoftRed
import com.brain.laesquinadelkafe.ui.utils.DateUtils
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun OrderCard(
    orderWithItems: OrderWithItems,
    onCardClick: () -> Unit,
    onStatusClick: () -> Unit = {},
    onPayClick: () -> Unit = {},
    onDeleteClick: (() -> Unit)? = null,
    onPartialPayClick: (() -> Unit)? = null,
    onUndoStatusClick: (() -> Unit)? = null,
    showActions: Boolean = true
) {
    val order = orderWithItems.order
    val items = orderWithItems.items
    
    var timeElapsed by remember { mutableStateOf("") }
    val actualCups = items.filter { !it.isToTakeAway }.sumOf { it.quantity }

    LaunchedEffect(key1 = order.timestamp) {
        while (true) {
            val diff = System.currentTimeMillis() - order.timestamp
            val minutes = diff / (1000 * 60)
            val hours = minutes / 60
            val days = hours / 24
            
            timeElapsed = when {
                days > 0 -> "${days}d ${hours % 24}h"
                hours > 0 -> "${hours}h ${minutes % 60}m"
                else -> "${minutes}min"
            }
            delay(60000)
        }
    }

    val diffMillis = System.currentTimeMillis() - order.timestamp
    val daysElapsed = diffMillis / (1000 * 60 * 60 * 24)
    
    val cardBgColor = if (order.status == "ENVIADO" && !order.isPaid) {
        when {
            daysElapsed >= 3 -> Color(0xFFFFCDD2)
            daysElapsed >= 1 -> Color(0xFFFFF9C4)
            else -> MaterialTheme.colorScheme.surface
        }
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        onClick = onCardClick,
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = order.clientName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (actualCups > 0 && !order.isPaid) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$actualCups",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fecha: ${DateUtils.formatDate(order.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = "Hora: ${DateUtils.formatTime(order.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                val remainingBalance = order.totalAmount - order.paidAmount
                Text(
                    text = "S/. ${String.format(Locale.getDefault(), "%.2f", if (order.isPaid) order.totalAmount else remainingBalance)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = if (!order.isPaid && order.paidAmount > 0) Color(0xFFC62828) else Color.Unspecified
                )
            }
            
            // Eliminado el bloque de "Taza(s) en uso" de abajo para ahorrar espacio
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!order.isPaid && order.status == "PENDIENTE") {
                    Surface(
                        color = SoftRed,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = order.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!order.isPaid && order.status == "ENVIADO") {
                    if (order.paidAmount > 0) {
                        Text(
                            text = "Pagado: S/. ${String.format(Locale.getDefault(), "%.2f", order.paidAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                
                if (showActions && !order.isPaid) {
                    Row {
                        if (order.status == "PENDIENTE") {
                            IconButton(onClick = onStatusClick) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = MaterialTheme.colorScheme.secondary)
                            }
                        } else if (order.status == "ENVIADO") {
                            onUndoStatusClick?.let {
                                IconButton(onClick = it) {
                                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Regresar a Pedidos", tint = Color.Gray)
                                }
                            }
                            onPartialPayClick?.let {
                                IconButton(onClick = it) {
                                    Icon(Icons.Default.Payments, contentDescription = "Pago Parcial", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        
                        onDeleteClick?.let {
                            IconButton(onClick = it) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        IconButton(onClick = onPayClick) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Pagar", tint = Color(0xFF43A047))
                        }
                    }
                }
            }
        }
    }
}
