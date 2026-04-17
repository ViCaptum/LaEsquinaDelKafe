package com.brain.laesquinadelkafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.brain.laesquinadelkafe.agent.CafeteriaAgent
import com.brain.laesquinadelkafe.agent.InventoryProvider
import com.brain.laesquinadelkafe.agent.ModelManager
import com.brain.laesquinadelkafe.agent.PromptBuilder
import com.brain.laesquinadelkafe.data.database.AppDatabase
import com.brain.laesquinadelkafe.data.repository.OrderRepository
import com.brain.laesquinadelkafe.data.repository.ProductRepository
import com.brain.laesquinadelkafe.ui.screens.*
import com.brain.laesquinadelkafe.ui.theme.LaEsquinaDelKafeTheme
import com.brain.laesquinadelkafe.viewmodel.*
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val database = AppDatabase.getDatabase(this)
            val orderRepository = OrderRepository(database.orderDao())
            val productRepository = ProductRepository(database.productDao())
            
            val orderViewModel: OrderViewModel = viewModel(factory = OrderViewModelFactory(orderRepository))
            val productViewModel: ProductViewModel = viewModel(factory = ProductViewModelFactory(productRepository))

            // Inicialización del Agente de IA
            val inventoryProvider = remember { InventoryProvider(productRepository) }
            val promptBuilder = remember { PromptBuilder() }
            val cafeteriaAgent = remember { CafeteriaAgent(productRepository, orderRepository, inventoryProvider, promptBuilder) }
            val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(cafeteriaAgent))
            
            val context = LocalContext.current
            val modelManager = remember { ModelManager(context) }

            LaEsquinaDelKafeTheme {
                val navController = rememberNavController()
                var selectedItem by remember { mutableIntStateOf(0) }
                val items = listOf("Chat", "Pedidos", "Deudas", "Historial", "Productos")
                val icons = listOf(
                    Icons.AutoMirrored.Filled.Chat,
                    Icons.Default.ListAlt,
                    Icons.Default.MoneyOff,
                    Icons.Default.History,
                    Icons.Default.Inventory
                )

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    icon = { Icon(icons[index], contentDescription = item) },
                                    label = { Text(item) },
                                    selected = selectedItem == index,
                                    onClick = {
                                        selectedItem = index
                                        navController.navigate(item)
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "Chat",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("Chat") { 
                            ChatScreen(chatViewModel, modelManager) 
                        }
                        composable("Pedidos") { OrdersScreen(orderViewModel, productViewModel) }
                        composable("Deudas") { DebtsScreen(orderViewModel) }
                        composable("Historial") { HistoryScreen(orderViewModel) }
                        composable("Productos") { ProductsScreen(productViewModel) }
                    }
                }
            }
        }
    }
}
