package com.brain.laesquinadelkafe.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brain.laesquinadelkafe.data.model.ProductEntity
import com.brain.laesquinadelkafe.viewmodel.ProductViewModel
import java.util.Locale

@Composable
fun ProductsScreen(viewModel: ProductViewModel) {
    val products by viewModel.products.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var showDuplicateProductDialog by remember { mutableStateOf<ProductEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Producto")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyColumn {
                items(products) { product ->
                    ProductItem(
                        product = product,
                        onToggleAvailability = { viewModel.updateProduct(product.copy(isAvailable = !product.isAvailable)) },
                        onEdit = { productToEdit = product },
                        onDelete = { productToDelete = product }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ProductDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, price, isDrink ->
                if (products.any { it.name.equals(name, ignoreCase = true) }) {
                    showDuplicateProductDialog = ProductEntity(name = name, price = price, isDrink = isDrink)
                } else {
                    viewModel.addProduct(name, price, isDrink)
                    showAddDialog = false
                }
            }
        )
    }

    if (productToEdit != null) {
        ProductDialog(
            product = productToEdit,
            onDismiss = { productToEdit = null },
            onConfirm = { name, price, isDrink ->
                viewModel.updateProduct(productToEdit!!.copy(name = name, price = price, isDrink = isDrink))
                productToEdit = null
            }
        )
    }

    showDuplicateProductDialog?.let { draft ->
        AlertDialog(
            onDismissRequest = { showDuplicateProductDialog = null },
            title = { Text("Producto Duplicado") },
            text = { Text("Ya existe un producto con el nombre '${draft.name}'. ¿Deseas crearlo de todas formas con un nombre diferente?") },
            confirmButton = {
                Button(onClick = {
                    var nextNumber = 2
                    val baseName = draft.name.replace(Regex(" \\d+$"), "")
                    while (products.any { it.name.equals(if (nextNumber == 1) baseName else "$baseName $nextNumber", ignoreCase = true) }) {
                        nextNumber++
                    }
                    viewModel.addProduct("$baseName $nextNumber", draft.price, draft.isDrink)
                    showDuplicateProductDialog = null
                    showAddDialog = false
                }) { Text("Crear como variante") }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateProductDialog = null }) { Text("Cancelar") }
            }
        )
    }

    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("¿Eliminar Producto?") },
            text = { Text("¿Estás seguro de que deseas eliminar '${product.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun ProductItem(
    product: ProductEntity,
    onToggleAvailability: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isAvailable) MaterialTheme.colorScheme.surface else Color.LightGray.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "S/. ${String.format(Locale.getDefault(), "%.2f", product.price)}", style = MaterialTheme.typography.bodyMedium)
                if (product.isDrink) {
                    Text(text = "Bebida", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row {
                IconButton(onClick = onToggleAvailability) {
                    Icon(
                        imageVector = if (product.isAvailable) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                        contentDescription = "Disponibilidad",
                        tint = if (product.isAvailable) Color(0xFF43A047) else Color.Gray
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ProductDialog(
    product: ProductEntity? = null,
    onDismiss: () -> Unit, 
    onConfirm: (String, Double, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var isDrink by remember { mutableStateOf(product?.isDrink ?: false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Nuevo Producto" else "Editar Producto") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.isEmpty() || it.replace(",", ".").toDoubleOrNull() != null) price = it },
                    label = { Text("Precio") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDrink, onCheckedChange = { isDrink = it })
                    Text("Es una bebida (Lleva taza)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val p = price.replace(",", ".").toDoubleOrNull()
                    if (name.isBlank()) {
                        Toast.makeText(context, "Falta nombre del producto", Toast.LENGTH_SHORT).show()
                    } else if (p == null) {
                        Toast.makeText(context, "Monto inválido", Toast.LENGTH_SHORT).show()
                    } else {
                        onConfirm(name, p, isDrink)
                    }
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
