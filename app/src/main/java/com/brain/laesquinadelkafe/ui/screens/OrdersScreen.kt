package com.brain.laesquinadelkafe.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.brain.laesquinadelkafe.data.model.OrderEntity
import com.brain.laesquinadelkafe.data.model.OrderItemEntity
import com.brain.laesquinadelkafe.data.model.OrderWithItems
import com.brain.laesquinadelkafe.ui.components.OrderCard
import com.brain.laesquinadelkafe.ui.components.ReceiptDialog
import com.brain.laesquinadelkafe.viewmodel.OrderViewModel
import com.brain.laesquinadelkafe.viewmodel.ProductViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun OrdersScreen(orderViewModel: OrderViewModel, productViewModel: ProductViewModel) {
    val orders by orderViewModel.pendingOrders.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedOrder by remember { mutableStateOf<OrderWithItems?>(null) }
    var orderToDelete by remember { mutableStateOf<OrderEntity?>(null) }
    var orderToPayDirectly by remember { mutableStateOf<OrderEntity?>(null) }
    var showDuplicateNameDialog by remember { mutableStateOf<Pair<String, List<OrderItemEntity>>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Pedido")
            }
        }
    ) { padding ->
        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay pedidos pendientes")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(orders) { orderWithItems ->
                    OrderCard(
                        orderWithItems = orderWithItems,
                        onCardClick = { selectedOrder = orderWithItems },
                        onStatusClick = { orderViewModel.updateStatus(orderWithItems.order, "ENVIADO") },
                        onPayClick = { orderToPayDirectly = orderWithItems.order },
                        onDeleteClick = { orderToDelete = orderWithItems.order }
                    )
                }
            }
        }
    }

    if (orderToPayDirectly != null) {
        AlertDialog(
            onDismissRequest = { orderToPayDirectly = null },
            title = { Text("¿Marcar como Pagado?") },
            text = { Text("El pedido de ${orderToPayDirectly!!.clientName} se moverá directamente al historial. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        orderViewModel.markAsPaid(orderToPayDirectly!!)
                        orderToPayDirectly = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                ) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { orderToPayDirectly = null }) { Text("Cancelar") }
            }
        )
    }

    if (orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("¿Eliminar Pedido?") },
            text = { Text("¿Estás seguro de que deseas eliminar el pedido de ${orderToDelete!!.clientName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        orderViewModel.deleteOrder(orderToDelete!!)
                        orderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    if (showAddDialog) {
        NewOrderDialog(
            productViewModel = productViewModel,
            onDismiss = { showAddDialog = false },
            onConfirm = { clientName, items ->
                scope.launch {
                    val existingOrder = orderViewModel.getActiveOrderByName(clientName)
                    if (existingOrder != null) {
                        showDuplicateNameDialog = clientName to items
                    } else {
                        orderViewModel.addOrder(clientName, items, false)
                    }
                }
                showAddDialog = false
            }
        )
    }

    showDuplicateNameDialog?.let { (name, items) ->
        AlertDialog(
            onDismissRequest = { showDuplicateNameDialog = null },
            title = { Text("Cliente Duplicado") },
            text = { Text("Ya existe un pedido pendiente para '$name'. ¿Es un cliente diferente o quieres agregar estos productos al pedido existente?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val allOrders = orderViewModel.pendingOrders.value + orderViewModel.debts.value
                        var nextNumber = 2
                        val baseName = name.replace(Regex(" \\d+$"), "")
                        
                        while (allOrders.any { it.order.clientName.equals(if (nextNumber == 1) baseName else "$baseName $nextNumber", ignoreCase = true) }) {
                            nextNumber++
                        }
                        
                        val newName = "$baseName $nextNumber"
                        orderViewModel.addOrder(newName, items, false)
                        showDuplicateNameDialog = null
                    }
                }) {
                    Text("Es diferente (Nuevo Cliente)")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        val existingOrder = orderViewModel.getActiveOrderByName(name)
                        existingOrder?.let {
                            val total = items.sumOf { it.quantity * it.pricePerUnit }
                            orderViewModel.addItemsToExistingOrder(it.order.id, items, total)
                        }
                        showDuplicateNameDialog = null
                    }
                }) {
                    Text("Es el mismo (Agregar al pedido)")
                }
            }
        )
    }

    selectedOrder?.let {
        ReceiptDialog(
            orderWithItems = it, 
            onDismiss = { selectedOrder = null },
            onDecreaseItemQuantity = { itemId ->
                orderViewModel.updateItemQuantity(itemId, it.items.find { item -> item.id == itemId }?.quantity?.minus(1) ?: 0)
            }
        )
    }
}

@Composable
fun NewOrderDialog(
    productViewModel: ProductViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, List<OrderItemEntity>) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    val products by productViewModel.products.collectAsState()
    val selectedItems = remember { mutableStateListOf<OrderItemEntity>() }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Pedido") },
        text = {
            Column(modifier = Modifier.heightIn(max = 450.dp)) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nombre del Cliente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text("Productos disponibles:", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(products.filter { it.isAvailable }) { product ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name)
                                Text("S/. ${String.format(Locale.getDefault(), "%.2f", product.price)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = {
                                    val isToTakeAway = !product.isDrink
                                    val existingIndex = selectedItems.indexOfFirst { it.productName == product.name && it.isToTakeAway == isToTakeAway }
                                    if (existingIndex != -1) {
                                        val item = selectedItems[existingIndex]
                                        selectedItems[existingIndex] = item.copy(quantity = item.quantity + 1)
                                    } else {
                                        selectedItems.add(
                                            OrderItemEntity(
                                                orderId = 0,
                                                productName = product.name,
                                                quantity = 1,
                                                pricePerUnit = product.price,
                                                isToTakeAway = isToTakeAway
                                            )
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Añadir")
                            }
                        }
                    }
                    
                    if (selectedItems.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text("Seleccionados:", style = MaterialTheme.typography.labelLarge)
                        }
                        itemsIndexed(selectedItems) { index, item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Text("${item.productName} x${item.quantity}", modifier = Modifier.weight(1f))
                                Text("S/. ${String.format(Locale.getDefault(), "%.2f", item.pricePerUnit * item.quantity)}", style = MaterialTheme.typography.bodySmall)
                                
                                val isDrink = products.find { it.name == item.productName }?.isDrink == true
                                if (isDrink) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Llevar", style = MaterialTheme.typography.labelSmall)
                                    Checkbox(
                                        checked = item.isToTakeAway,
                                        onCheckedChange = { checked ->
                                            selectedItems[index] = item.copy(isToTakeAway = checked)
                                        }
                                    )
                                }
                                IconButton(onClick = { selectedItems.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = Color.Red, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (clientName.isBlank()) {
                    Toast.makeText(context, "Falta nombre del cliente", Toast.LENGTH_SHORT).show()
                } else if (selectedItems.isEmpty()) {
                    Toast.makeText(context, "Falta añadir productos", Toast.LENGTH_SHORT).show()
                } else {
                    onConfirm(clientName, selectedItems)
                }
            }) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
