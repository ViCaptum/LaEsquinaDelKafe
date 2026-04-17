package com.brain.laesquinadelkafe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brain.laesquinadelkafe.data.model.OrderEntity
import com.brain.laesquinadelkafe.data.model.OrderWithItems
import com.brain.laesquinadelkafe.ui.components.OrderCard
import com.brain.laesquinadelkafe.ui.components.ReceiptDialog
import com.brain.laesquinadelkafe.viewmodel.OrderViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DebtsScreen(viewModel: OrderViewModel) {
    val debts by viewModel.debts.collectAsState()
    var selectedOrder by remember { mutableStateOf<OrderWithItems?>(null) }
    var orderToPayPartially by remember { mutableStateOf<OrderEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (debts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay deudas pendientes")
                }
            } else {
                LazyColumn {
                    items(debts) { orderWithItems ->
                        OrderCard(
                            orderWithItems = orderWithItems,
                            onCardClick = { selectedOrder = orderWithItems },
                            onPayClick = { 
                                val cups = orderWithItems.items.filter { !it.isToTakeAway }.sumOf { it.quantity }
                                if (cups > 0) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("No se puede cerrar: Faltan devolver $cups tazas")
                                    }
                                    selectedOrder = orderWithItems 
                                } else {
                                    viewModel.markAsPaid(orderWithItems.order) 
                                }
                            },
                            onDeleteClick = null,
                            onPartialPayClick = { orderToPayPartially = orderWithItems.order },
                            onUndoStatusClick = { viewModel.updateStatus(orderWithItems.order, "PENDIENTE") }
                        )
                    }
                }
            }
        }
    }

    if (orderToPayPartially != null) {
        val order = orderToPayPartially!!
        var amountText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { orderToPayPartially = null },
            title = { Text("Registrar Pago Parcial") },
            text = {
                Column {
                    Text("Deuda Total: S/. ${String.format(Locale.getDefault(), "%.2f", order.totalAmount)}")
                    Text("Ya pagado: S/. ${String.format(Locale.getDefault(), "%.2f", order.paidAmount)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.replace(",", ".").toDoubleOrNull() != null) {
                                amountText = newValue
                            }
                        },
                        label = { Text("Monto a pagar ahora") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val additionalPayment = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (additionalPayment > 0) {
                        val newPaidAmount = order.paidAmount + additionalPayment
                        val isFullyPaid = newPaidAmount >= order.totalAmount
                        viewModel.updatePayment(order.id, newPaidAmount, isFullyPaid)
                        orderToPayPartially = null
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { orderToPayPartially = null }) { Text("Cancelar") }
            }
        )
    }

    selectedOrder?.let {
        ReceiptDialog(
            orderWithItems = it, 
            onDismiss = { selectedOrder = null },
            onDecreaseItemQuantity = { itemId ->
                viewModel.updateItemQuantity(itemId, it.items.find { item -> item.id == itemId }?.quantity?.minus(1) ?: 0)
            }
        )
    }
}
