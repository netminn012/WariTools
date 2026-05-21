package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.Member

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    viewModel: BillViewModel,
    innerPadding: PaddingValues
) {
    val members by viewModel.members.collectAsState()
    var inputName by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var editName by remember { mutableStateOf("") }

    var inputMode by remember { mutableStateOf(0) } // 0: 名前を入力, 1: 人数のみ指定
    var bulkCount by remember { mutableStateOf(4) } // デフォルト 4人

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
    ) {
        // Upper Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.member_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (members.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearAllMembers() },
                    modifier = Modifier.testTag("clear_all_members_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "Clear All",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                    Text("名前を入力")
                }
            }
            SegmentedButton(
                selected = inputMode == 1,
                onClick = { inputMode = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("人数のみ指定")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (inputMode == 0) {
            // Input Card Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text(stringResource(R.string.member_hint)) },
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(end = 8.dp)
                            .testTag("member_name_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            viewModel.addMember(inputName)
                            inputName = ""
                            focusManager.clearFocus()
                        }),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.addMember(inputName)
                            inputName = ""
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("add_member_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Member")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.member_add_btn))
                    }
                }
            }
        } else {
            // Bulk Count selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "人数のみで一括登録",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "名前を入力しない場合に、自動的に「1番」「2番」…として割り勘用の仮メンバーを登録します。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { if (bulkCount > 1) bulkCount-- },
                            enabled = bulkCount > 1,
                            modifier = Modifier
                                .background(
                                    color = if (bulkCount > 1) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .size(40.dp)
                        ) {
                            Text(
                                "－",
                                fontWeight = FontWeight.Bold,
                                color = if (bulkCount > 1) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        Text(
                            text = "${bulkCount}人",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        IconButton(
                            onClick = { if (bulkCount < 20) bulkCount++ },
                            enabled = bulkCount < 20,
                            modifier = Modifier
                                .background(
                                    color = if (bulkCount < 20) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .size(40.dp)
                        ) {
                            Text(
                                "＋",
                                fontWeight = FontWeight.Bold,
                                color = if (bulkCount < 20) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.addMembersByCount(bulkCount)
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("bulk_member_add_btn"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "${bulkCount}人を登録して割り勘を開始する",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Display area (encouraged by prompt)
        if (members.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.member_empty_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members, key = { it.id }) { member ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    memberToEdit = member
                                    editName = member.name
                                },
                                onLongClick = {
                                    viewModel.deleteMember(member)
                                }
                            )
                            .testTag("member_card_${member.id}"),
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
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1.0f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                // Direct edit icon
                                Row {
                                    IconButton(
                                        onClick = {
                                            memberToEdit = member
                                            editName = member.name
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("edit_member_${member.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Member",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteMember(member) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_member_${member.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Member",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
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

    // Edit Member Dialog
    if (memberToEdit != null) {
        AlertDialog(
            onDismissRequest = { memberToEdit = null },
            title = { Text(stringResource(R.string.member_edit_title)) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.member_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_member_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        memberToEdit?.let {
                            if (editName.isNotBlank()) {
                                viewModel.updateMember(it.copy(name = editName.trim()))
                            }
                        }
                        memberToEdit = null
                    },
                    modifier = Modifier.testTag("save_member_btn")
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { memberToEdit = null }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
