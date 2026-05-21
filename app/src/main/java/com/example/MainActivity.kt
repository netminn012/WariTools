package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core Room construction
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BillRepository(
            memberDao = database.memberDao(),
            billItemDao = database.billItemDao(),
            billSettingsDao = database.billSettingsDao(),
            geminiRepository = GeminiRepository()
        )

        setContent {
            val viewModel: BillViewModel by viewModels { BillViewModelFactory(repository) }
            var selectedTab by remember { mutableIntStateOf(0) }

            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                label = { Text(stringResource(R.string.tab_members)) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 0) Icons.Default.People else Icons.Outlined.People,
                                        contentDescription = stringResource(R.string.tab_members)
                                    )
                                },
                                modifier = Modifier.testTag("tab_members_btn")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                label = { Text(stringResource(R.string.tab_items)) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Default.Receipt else Icons.Outlined.Receipt,
                                        contentDescription = stringResource(R.string.tab_items)
                                    )
                                },
                                modifier = Modifier.testTag("tab_items_btn")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                label = { Text(stringResource(R.string.tab_settlement)) },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == 2) Icons.Default.Calculate else Icons.Outlined.Calculate,
                                        contentDescription = stringResource(R.string.tab_settlement)
                                    )
                                },
                                modifier = Modifier.testTag("tab_settlement_btn")
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> MembersScreen(viewModel, innerPadding)
                        1 -> ItemsScreen(viewModel, innerPadding)
                        2 -> SettlementScreen(viewModel, innerPadding)
                    }
                }
            }
        }
    }
}

