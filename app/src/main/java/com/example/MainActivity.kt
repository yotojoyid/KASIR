package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CashierScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PosViewModel

enum class MainNavDestination(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    CASHIER("Kasir", Icons.Default.PointOfSale, "nav_cashier"),
    PRODUCTS("Produk", Icons.Default.Inventory2, "nav_products"),
    REPORTS("Laporan", Icons.Default.Assessment, "nav_reports"),
    CLOUD_SYNC("Cloud", Icons.Default.CloudSync, "nav_cloud")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val posViewModel: PosViewModel = viewModel()
                val storeConfig by posViewModel.storeConfig.collectAsStateWithLifecycle()
                var currentDestination by remember { mutableStateOf(MainNavDestination.CASHIER) }

                // System back handling: return to Cashier screen first if on other tabs
                if (currentDestination != MainNavDestination.CASHIER) {
                    BackHandler {
                        currentDestination = MainNavDestination.CASHIER
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    // Automatically adopt desktop canonical layout on tablets / desktops (>= 720dp)
                    // or when Desktop Mode is toggled by user
                    val isDesktopLayout = maxWidth >= 720.dp || storeConfig.isDesktopModePreferred

                    if (isDesktopLayout) {
                        // Desktop & Tablet Canonical Layout: Left NavigationRail + Dual-Pane Content
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail(
                                header = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PointOfSale,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Desktop POS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxHeight(),
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                MainNavDestination.entries.forEach { destination ->
                                    val isSelected = currentDestination == destination
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = { currentDestination = destination },
                                        icon = {
                                            Icon(
                                                imageVector = destination.icon,
                                                contentDescription = destination.title
                                            )
                                        },
                                        label = { Text(destination.title, fontSize = 10.sp) },
                                        modifier = Modifier.testTag(destination.testTag)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Toggle mode button at bottom of Rail
                                IconButton(
                                    onClick = { posViewModel.toggleDesktopMode() },
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Smartphone,
                                        contentDescription = "Beralih ke Tampilan Ponsel",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                when (currentDestination) {
                                    MainNavDestination.CASHIER -> {
                                        CashierScreen(
                                            viewModel = posViewModel,
                                            isDesktopLayout = true,
                                            onNavigateToSync = {
                                                currentDestination = MainNavDestination.CLOUD_SYNC
                                            }
                                        )
                                    }
                                    MainNavDestination.PRODUCTS -> {
                                        ProductsScreen(viewModel = posViewModel)
                                    }
                                    MainNavDestination.REPORTS -> {
                                        ReportsScreen(viewModel = posViewModel)
                                    }
                                    MainNavDestination.CLOUD_SYNC -> {
                                        CloudSyncScreen(viewModel = posViewModel)
                                    }
                                }
                            }
                        }
                    } else {
                        // Handheld Mobile Layout with Bottom NavigationBar
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ) {
                                    MainNavDestination.entries.forEach { destination ->
                                        val isSelected = currentDestination == destination
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { currentDestination = destination },
                                            icon = {
                                                Icon(
                                                    imageVector = destination.icon,
                                                    contentDescription = destination.title
                                                )
                                            },
                                            label = {
                                                Text(text = destination.title, fontSize = 11.sp)
                                            },
                                            modifier = Modifier.testTag(destination.testTag),
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentDestination) {
                                    MainNavDestination.CASHIER -> {
                                        CashierScreen(
                                            viewModel = posViewModel,
                                            isDesktopLayout = false,
                                            onNavigateToSync = {
                                                currentDestination = MainNavDestination.CLOUD_SYNC
                                            }
                                        )
                                    }
                                    MainNavDestination.PRODUCTS -> {
                                        ProductsScreen(viewModel = posViewModel)
                                    }
                                    MainNavDestination.REPORTS -> {
                                        ReportsScreen(viewModel = posViewModel)
                                    }
                                    MainNavDestination.CLOUD_SYNC -> {
                                        CloudSyncScreen(viewModel = posViewModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
