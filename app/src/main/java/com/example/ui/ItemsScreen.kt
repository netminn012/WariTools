package com.example.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.BillItem
import com.example.data.Member

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(
    viewModel: BillViewModel,
    innerPadding: PaddingValues
) {
    val items by viewModel.billItems.collectAsState()
    val members by viewModel.members.collectAsState()
    val scanLoading by viewModel.scanLoading.collectAsState()
    val scanError by viewModel.scanError.collectAsState()

    val context = LocalContext.current
    var inputMode by remember { mutableIntStateOf(0) } // 0: 手入力, 1: レシートスキャン

    var showFormDialog by remember { mutableStateOf(false) }
    var activeEditingItem by remember { mutableStateOf<BillItem?>(null) }

    // Selected image display state
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Launcher for Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val resolver = context.contentResolver
                val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                    val src = ImageDecoder.createSource(resolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(resolver, uri)
                }
                selectedBitmap = bitmap
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    Toast.makeText(context, "AI Studioの秘密キー設定パネルでAPIキーを登録してください。", Toast.LENGTH_LONG).show()
                } else {
                    viewModel.scanReceipt(bitmap, apiKey)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "画像の保存に失敗しました。", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                Toast.makeText(context, "Google AI Studioの秘密キーパネルからAPIキーを設定してください。", Toast.LENGTH_LONG).show()
            } else {
                viewModel.scanReceipt(bitmap, apiKey)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Selector segment
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = inputMode == 0,
                onClick = { inputMode = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.item_mode_manual))
                }
            }
            SegmentedButton(
                selected = inputMode == 1,
                onClick = { inputMode = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.item_mode_receipt))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (inputMode == 0) {
            // --- Manual input / Hand mode List ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.item_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (items.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllBillItems() },
                        modifier = Modifier.testTag("clear_all_items_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All Items",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ListAlt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.item_empty_state),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            members = members,
                            onClick = {
                                activeEditingItem = item
                                showFormDialog = true
                            },
                            onDelete = { viewModel.deleteBillItem(item) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add FAB Action
            FloatingActionButton(
                onClick = {
                    activeEditingItem = null
                    showFormDialog = true
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("add_item_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }

        } else {
            // --- Scan mode Layout ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "Scan Icon",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.receipt_instruction),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (scanLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.receipt_scanning),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    cameraLauncher.launch()
                                },
                                modifier = Modifier
                                    .weight(1.0f)
                                    .padding(horizontal = 8.dp)
                                    .testTag("capture_camera_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.camera_pick))
                            }

                            Button(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1.0f)
                                    .padding(horizontal = 8.dp)
                                    .testTag("select_gallery_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.gallery_pick))
                            }
                        }
                    }

                    if (scanError != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = scanError ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1.0f)
                                )
                                IconButton(onClick = { viewModel.clearScanError() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // Show scanned success feedback
                    if (selectedBitmap != null && !scanLoading && scanError == null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "スキャン完了！品目一覧に追加されました。手入力タブで割り当てを確認・編集してください。",
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Sponsor space
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

    // Add / Edit Dialog details
    if (showFormDialog) {
        ItemFormDialog(
            item = activeEditingItem,
            members = members,
            onDismiss = { showFormDialog = false },
            onSave = { name, amount, splitType, assignedIds, isTaxIncl, taxR ->
                if (activeEditingItem == null) {
                    viewModel.addBillItem(name, amount, splitType, assignedIds, isTaxIncl, taxR)
                } else {
                    viewModel.updateBillItem(activeEditingItem!!.id, name, amount, splitType, assignedIds, isTaxIncl, taxR)
                }
                showFormDialog = false
            }
        )
    }
}

@Composable
fun ItemCard(
    item: BillItem,
    members: List<Member>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("item_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant vertical geometric accent bar (border-l-4 border-[#D0BCFF] equivalent)
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val splitDesc = when (item.splitType) {
                        "ALL_EQUAL" -> stringResource(R.string.item_split_all_equal)
                        "INDIVIDUAL" -> {
                            val associatedNames = members.filter { item.assignedMemberIds.contains(it.id) }.map { it.name }
                            if (associatedNames.isEmpty()) {
                                "未指定 (全員)"
                            } else {
                                "個人 (${associatedNames.firstOrNull()})"
                            }
                        }
                        "SELECTED_EQUAL" -> {
                            val names = members.filter { item.assignedMemberIds.contains(it.id) }.map { it.name }
                            "シェア (${names.size}人)"
                        }
                        else -> ""
                    }
                    val taxLabel = if (item.isTaxIncluded) " (税込)" else " (税抜 ${item.taxRate}%)"

                    SuggestionChip(
                        onClick = onClick,
                        label = { Text(splitDesc, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = taxLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%,.0f円", item.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_item_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemFormDialog(
    item: BillItem?,
    members: List<Member>,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, splitType: String, assignedMemberIds: List<Int>, isTaxIncluded: Boolean, taxRate: Int) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var amountStr by remember { mutableStateOf(item?.amount?.let { if (it % 1 == 0.0) String.format("%.0f", it) else it.toString() } ?: "") }
    var isTaxIncluded by remember { mutableStateOf(item?.isTaxIncluded ?: true) }
    var taxRate by remember { mutableIntStateOf(item?.taxRate ?: 10) }
    var splitType by remember { mutableStateOf(item?.splitType ?: "ALL_EQUAL") } // "ALL_EQUAL", "INDIVIDUAL", "SELECTED_EQUAL"

    // Set of member IDs that are currently assigned
    val assignedIds = remember { mutableStateListOf<Int>().apply { addAll(item?.assignedMemberIds ?: emptyList()) } }

    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "品目情報の追加" else "品目情報の変更") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Input
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.item_name_hint)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_name_dialog_input")
                    )
                }

                // Amount Input
                item {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            inputError = null
                        },
                        label = { Text(stringResource(R.string.item_amount_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_amount_dialog_input"),
                        isError = inputError != null,
                    )
                    if (inputError != null) {
                        Text(
                            text = inputError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // Tax Configuration Part
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.item_tax_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isTaxIncluded,
                                    onClick = { isTaxIncluded = true },
                                    modifier = Modifier.testTag("tax_inc_radio")
                                )
                                Text(
                                    stringResource(R.string.item_tax_included),
                                    modifier = Modifier.clickable { isTaxIncluded = true }
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = !isTaxIncluded,
                                    onClick = { isTaxIncluded = false },
                                    modifier = Modifier.testTag("tax_ex_radio")
                                )
                                Text(
                                    stringResource(R.string.item_tax_excluded),
                                    modifier = Modifier.clickable { isTaxIncluded = false }
                                )
                            }
                        }
                    }
                }

                // If tax excluded: Tax rate selector
                if (!isTaxIncluded) {
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.item_tax_rate_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = taxRate == 8,
                                        onClick = { taxRate = 8 }
                                    )
                                    Text(
                                        stringResource(R.string.item_tax_rate_8),
                                        modifier = Modifier.clickable { taxRate = 8 },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = taxRate == 10,
                                        onClick = { taxRate = 10 }
                                    )
                                    Text(
                                        stringResource(R.string.item_tax_rate_10),
                                        modifier = Modifier.clickable { taxRate = 10 },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Split type selectors
                item {
                    Column {
                        Text(
                            text = stringResource(R.string.item_split_type_label),
                            style = MaterialTheme.typography.labelWithSubtitle()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = splitType == "ALL_EQUAL",
                                onClick = {
                                    splitType = "ALL_EQUAL"
                                    assignedIds.clear()
                                },
                                label = { Text(stringResource(R.string.item_split_all_equal)) },
                                modifier = Modifier.testTag("chip_all")
                            )

                            FilterChip(
                                selected = splitType == "INDIVIDUAL",
                                onClick = {
                                    splitType = "INDIVIDUAL"
                                    if (assignedIds.isEmpty() && members.isNotEmpty()) {
                                        assignedIds.add(members.first().id)
                                    } else if (assignedIds.size > 1 && members.isNotEmpty()) {
                                        val first = assignedIds.first()
                                        assignedIds.clear()
                                        assignedIds.add(first)
                                    }
                                },
                                label = { Text(stringResource(R.string.item_split_individual)) },
                                modifier = Modifier.testTag("chip_individual")
                            )

                            FilterChip(
                                selected = splitType == "SELECTED_EQUAL",
                                onClick = {
                                    splitType = "SELECTED_EQUAL"
                                },
                                label = { Text(stringResource(R.string.item_split_selected)) },
                                modifier = Modifier.testTag("chip_group")
                            )
                        }
                    }
                }

                // Member assigning checkboxes
                if (splitType == "INDIVIDUAL") {
                    item {
                        Column {
                            Text(
                                text = "対象メンバーの指定 (1人)",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (members.isEmpty()) {
                                Text("メンバーが登録されていません。メンバー画面から追加してください。")
                            } else {
                                Column {
                                    members.forEach { m ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    assignedIds.clear()
                                                    assignedIds.add(m.id)
                                                }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            RadioButton(
                                                selected = assignedIds.contains(m.id),
                                                onClick = {
                                                    assignedIds.clear()
                                                    assignedIds.add(m.id)
                                                },
                                                modifier = Modifier.testTag("radio_member_${m.id}")
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(m.name)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (splitType == "SELECTED_EQUAL") {
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.item_assigned_members_label),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (members.isEmpty()) {
                                Text("メンバーが登録されていません。メンバー画面から追加してください。")
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    members.forEach { member ->
                                        val isAssigned = assignedIds.contains(member.id)
                                        InputChip(
                                            selected = isAssigned,
                                            onClick = {
                                                if (isAssigned) {
                                                    assignedIds.remove(member.id)
                                                } else {
                                                    assignedIds.add(member.id)
                                                }
                                            },
                                            label = { Text(member.name) },
                                            leadingIcon = {
                                                if (isAssigned) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                } else {
                                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            modifier = Modifier.testTag("chk_member_${member.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount == null || amount < 0.0) {
                        inputError = "半角数字を入力してください。"
                    } else if (name.isBlank()) {
                        inputError = "品目名を入力してください。"
                    } else if (splitType == "INDIVIDUAL" && assignedIds.size != 1) {
                        inputError = "対象メンバーを1名選択してください。"
                    } else if (splitType == "SELECTED_EQUAL" && assignedIds.isEmpty()) {
                        inputError = "シェアするメンバーを1名以上選択してください。"
                    } else {
                        onSave(
                            name,
                            amount,
                            splitType,
                            assignedIds.toList(),
                            isTaxIncluded,
                            taxRate
                        )
                    }
                },
                modifier = Modifier.testTag("item_dialog_save")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun Typography.labelWithSubtitle() = labelMedium.copy(
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight = FontWeight.Bold
)
