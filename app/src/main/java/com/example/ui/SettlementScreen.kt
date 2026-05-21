package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BillSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    viewModel: BillViewModel,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val members by viewModel.members.collectAsState()
    val settingsState by viewModel.settings.collectAsState()
    val rawSettings = settingsState ?: BillSettings()

    val settlement = viewModel.calculateSettlement()

    // Create the copyable text block based on requested specification format
    val copyableText = remember(settlement) {
        val sb = StringBuilder()
        sb.append("【WariTools精算結果】\n")
        settlement.memberPayments.forEach { p ->
            val cleanBreakdown = p.breakdown
                .replace("・", ", ")
                .replace(":", " ")
            sb.append("${p.member.name}：${String.format("%,d", p.amount)}円（$cleanBreakdown）\n")
        }
        sb.append("合計：${String.format("%,d", settlement.totalAmount)}円")
        if (settlement.isTaxIncluded) {
            sb.append("（税込）")
        } else {
            sb.append("（税込）") // standard JP representation
        }
        sb.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settlement_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (members.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.settlement_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            // Summary banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "合計精算額",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = String.format("%,d円", settlement.totalAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Toggles to switch modes
                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                rawSettings.copy(isPureEqualSplitMode = !rawSettings.isPureEqualSplitMode)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("toggle_equal_split_mode")
                    ) {
                        Icon(
                            imageVector = if (rawSettings.isPureEqualSplitMode) Icons.Default.GridOn else Icons.Default.CallSplit,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (rawSettings.isPureEqualSplitMode) {
                                "品目割りに戻す"
                            } else {
                                "全員均等割り"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Settlement list
            LazyColumn(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(settlement.memberPayments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Geometric Accent line
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )

                            Row(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.0f)) {
                                    Text(
                                        text = payment.member.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = payment.breakdown,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = String.format("%,d円", payment.amount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                // Configuration Settings Row
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settlement_option_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Fraction Payer Selection Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settlement_fraction_payer),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = rawSettings.fractionPayerType == "ORGANIZER",
                                    onClick = {
                                        viewModel.updateSettings(rawSettings.copy(fractionPayerType = "ORGANIZER"))
                                    },
                                    label = { Text("幹事 (1番)") },
                                    modifier = Modifier.testTag("faction_organizer_chip")
                                )
                                FilterChip(
                                    selected = rawSettings.fractionPayerType == "RANDOM",
                                    onClick = {
                                        viewModel.updateSettings(rawSettings.copy(fractionPayerType = "RANDOM"))
                                    },
                                    label = { Text("ランダム") },
                                    modifier = Modifier.testTag("faction_random_chip")
                                )
                                FilterChip(
                                    selected = rawSettings.fractionPayerType == "MANUAL",
                                    onClick = {
                                        viewModel.updateSettings(rawSettings.copy(fractionPayerType = "MANUAL"))
                                    },
                                    label = { Text("手動指定") },
                                    modifier = Modifier.testTag("faction_manual_chip")
                                )
                            }

                            if (rawSettings.fractionPayerType == "MANUAL") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "多く払うメンバーの選択:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    members.forEach { m ->
                                        val isSelectedPayer = rawSettings.manualFractionPayerId == m.id
                                        InputChip(
                                            selected = isSelectedPayer,
                                            onClick = {
                                                viewModel.updateSettings(rawSettings.copy(manualFractionPayerId = m.id))
                                            },
                                            label = { Text(m.name) },
                                            modifier = Modifier.testTag("payer_sel_${m.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Consumption Tax split strategy Card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "消費税の精算方法を選択",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = rawSettings.taxSplitType == "PRORATED",
                                    onClick = {
                                        viewModel.updateSettings(rawSettings.copy(taxSplitType = "PRORATED"))
                                    },
                                    label = { Text("注文額の比率で按分") },
                                    modifier = Modifier.testTag("tax_prorated_chip")
                                )

                                FilterChip(
                                    selected = rawSettings.taxSplitType == "EQUAL",
                                    onClick = {
                                        viewModel.updateSettings(rawSettings.copy(taxSplitType = "EQUAL"))
                                    },
                                    label = { Text("人数で均等割り") },
                                    modifier = Modifier.testTag("tax_equal_chip")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Share & Copy Trigger Layout Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("WariTools Settlement", copyableText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.settlement_copied_toast), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1.0f)
                        .height(50.dp)
                        .testTag("copy_settlement_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settlement_copy_btn))
                }

                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, copyableText)
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "精算結果を送る")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .weight(1.0f)
                        .height(50.dp)
                        .testTag("share_settlement_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settlement_share_btn))
                }
            }
        }

        // Sponsor Banner Space (For future AdMob integration)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.ad_banner_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
