package com.squidink.alloy.modules.scratch.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.squidink.alloy.core.design.R
import com.squidink.alloy.core.domain.common.repository.Scratch
import com.squidink.alloy.modules.scratch.ScratchUiAction
import com.squidink.alloy.modules.scratch.ScratchUiEffect
import com.squidink.alloy.modules.scratch.ScratchViewModel
import com.squidink.alloy.modules.scratch.auth.BiometricPromptManager
import com.squidink.alloy.modules.scratch.model.EditorLanguage
import com.squidink.alloy.modules.scratch.ui.syntax.HighlightsVisualTransformation
import com.squidink.alloy.modules.scratch.util.DocumentExporter
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import kotlinx.coroutines.launch

@Composable
fun ScratchScreen(
    viewModel: ScratchViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val biometricPromptManager = remember(context) { BiometricPromptManager(context) }

    DisposableEffect(biometricPromptManager) {
        onDispose {
            biometricPromptManager.cancel()
        }
    }

    // Auto-lock sensitive documents on app backgrounding (ON_STOP)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAction(ScratchUiAction.LockAllDocuments)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var documentToRename by remember { mutableStateOf<Scratch?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingExportData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/*")
    ) { uri ->
        val exportData = pendingExportData
        if (uri != null && exportData != null) {
            val (fileName, _, content) = exportData
            scope.launch {
                val result = DocumentExporter.exportToUri(context.contentResolver, uri, content)
                result.fold(
                    onSuccess = {
                        viewModel.onAction(
                            ScratchUiAction.SetUserMessage(
                                context.getString(R.string.scratch_export_success)
                            )
                        )
                    },
                    onFailure = { error ->
                        viewModel.onAction(
                            ScratchUiAction.SetUserMessage(
                                context.getString(
                                    R.string.scratch_export_error,
                                    error.localizedMessage ?: "Unknown error"
                                )
                            )
                        )
                    }
                )
            }
        }
        pendingExportData = null
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ScratchUiEffect.RequestBiometricAuth -> {
                    val doc = uiState.documents.find { it.id == effect.documentId }
                    val title = doc?.title ?: "Document"
                    biometricPromptManager.promptAuth(
                        title = "Unlock $title",
                        subtitle = "Authenticate with biometric or screen lock",
                        onSuccess = {
                            viewModel.onAction(
                                ScratchUiAction.AuthenticateDocumentSuccess(effect.documentId)
                            )
                        }
                    )
                }
                is ScratchUiEffect.LaunchExportPicker -> {
                    pendingExportData = Triple(effect.fileName, effect.mimeType, effect.content)
                    exportLauncher.launch(effect.fileName)
                }
            }
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onAction(ScratchUiAction.ClearMessage)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
        // Header with Timer and Lock Vault quick action
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.scratch_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )

            if (uiState.unlockedDocumentIds.isNotEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.onAction(ScratchUiAction.LockAllDocuments) },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.scratch_lock_vault),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.scratch_lock_vault))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Document Workspace
        Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Multi-Document Tab Bar
                        if (uiState.documents.isNotEmpty()) {
                            DocumentTabBar(
                                documents = uiState.documents,
                                activeDocumentId = uiState.activeDocumentId,
                                unlockedDocumentIds = uiState.unlockedDocumentIds,
                                onSelectDocument = { viewModel.onAction(ScratchUiAction.SelectDocument(it)) },
                                onCloseDocument = { viewModel.onAction(ScratchUiAction.CloseDocument(it)) },
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val activeDoc = uiState.activeDocument
                            if (activeDoc != null) {
                                EditorToolbar(
                                    document = activeDoc,
                                    isUnlockedInSession = uiState.unlockedDocumentIds.contains(activeDoc.id),
                                    onLanguageChange = { lang ->
                                        viewModel.onAction(ScratchUiAction.SetDocumentLanguage(activeDoc.id, lang))
                                    },
                                    onToggleLock = {
                                        viewModel.onAction(ScratchUiAction.RequestToggleLock(activeDoc.id))
                                    },
                                    onExportClick = {
                                        viewModel.onAction(ScratchUiAction.RequestExportDocument(activeDoc.id))
                                    },
                                    onRenameClick = { documentToRename = activeDoc }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (uiState.isCurrentDocumentLocked) {
                                    LockboxGatekeeperPane(
                                        document = activeDoc,
                                        onUnlock = {
                                            biometricPromptManager?.promptAuth(
                                                title = "Unlock ${activeDoc.title}",
                                                subtitle = "Authenticate with biometric or screen lock",
                                                onSuccess = {
                                                    viewModel.onAction(
                                                        ScratchUiAction.AuthenticateDocumentSuccess(activeDoc.id)
                                                    )
                                                }
                                            )
                                        }
                                    )
                                } else {
                                    CodeEditorPane(
                                        content = activeDoc.content,
                                        language = EditorLanguage.fromName(activeDoc.language),
                                        onContentChange = { viewModel.onAction(ScratchUiAction.UpdateContent(it)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            // Empty documents state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "No open documents",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { viewModel.onAction(ScratchUiAction.CreateNewDocument) }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.scratch_new_doc))
                                    }
                                }
                            }
                        }
                    }

                    // Floating Action Button (FAB) pattern for creating new documents
                    FloatingActionButton(
                        onClick = { viewModel.onAction(ScratchUiAction.CreateNewDocument) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.scratch_new_doc)
                        )
                    }
                }
    }

    // Dialog: Lockbox confirmation explaining encryption and re-lock rules
    if (uiState.showLockConfirmationDialog && uiState.pendingLockDocumentId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(ScratchUiAction.DismissLockConfirmation) },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.scratch_lockbox_confirm_title)) },
            text = { Text(stringResource(R.string.scratch_lockbox_confirm_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onAction(ScratchUiAction.ConfirmLockDocument(uiState.pendingLockDocumentId!!))
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(ScratchUiAction.DismissLockConfirmation) }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Dialog: Rename Document
    documentToRename?.let { doc ->
        var newTitle by remember(doc) { mutableStateOf(doc.title) }
        AlertDialog(
            onDismissRequest = { documentToRename = null },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    label = { Text("Document Title") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.onAction(ScratchUiAction.RenameDocument(doc.id, newTitle))
                        }
                        documentToRename = null
                    }
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToRename = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
        }
    }
}

@Composable
private fun DocumentTabBar(
    documents: List<Scratch>,
    activeDocumentId: String?,
    unlockedDocumentIds: Set<String>,
    onSelectDocument: (String) -> Unit,
    onCloseDocument: (String) -> Unit,
) {
    val selectedIndex = documents.indexOfFirst { it.id == activeDocumentId }.coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        documents.forEach { doc ->
            val isSelected = doc.id == activeDocumentId
            val isLocked = doc.isLocked
            val isUnlockedInSession = unlockedDocumentIds.contains(doc.id)

            Tab(
                selected = isSelected,
                onClick = { onSelectDocument(doc.id) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isLocked) {
                            Icon(
                                imageVector = if (isUnlockedInSession) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = if (isUnlockedInSession) "Unlocked for session" else "Locked",
                                tint = if (isUnlockedInSession) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = doc.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (documents.size > 1) {
                            IconButton(
                                onClick = { onCloseDocument(doc.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.scratch_close_doc),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    document: Scratch,
    isUnlockedInSession: Boolean,
    onLanguageChange: (EditorLanguage) -> Unit,
    onToggleLock: () -> Unit,
    onExportClick: () -> Unit,
    onRenameClick: () -> Unit,
) {
    var expandedDropdown by remember { mutableStateOf(false) }
    val currentLanguage = EditorLanguage.fromName(document.language)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Document rename button
        IconButton(onClick = onRenameClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Rename Document", modifier = Modifier.size(16.dp))
        }

        // Language Dropdown Selector
        Box {
            AssistChip(
                onClick = { expandedDropdown = true },
                label = { Text("${stringResource(R.string.scratch_format_label)}: ${currentLanguage.displayName}") }
            )

            DropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false }
            ) {
                EditorLanguage.entries.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.displayName) },
                        onClick = {
                            onLanguageChange(lang)
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Export Document Action
        AssistChip(
            onClick = onExportClick,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = {
                Text(stringResource(R.string.scratch_export_doc))
            }
        )

        // Lock / Unlock Lockbox Action
        AssistChip(
            onClick = onToggleLock,
            leadingIcon = {
                Icon(
                    imageVector = if (document.isLocked) Icons.Default.LockOpen else Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            label = {
                Text(
                    if (document.isLocked) {
                        stringResource(R.string.scratch_unlock_doc)
                    } else {
                        stringResource(R.string.scratch_lock_doc)
                    }
                )
            }
        )
    }
}

@Composable
private fun CodeEditorPane(
    content: String,
    language: EditorLanguage,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()

    // Configure Highlights engine based on language and theme
    val highlights = remember(content, language, isDark) {
        language.syntaxLanguage?.let { syntaxLang ->
            Highlights.Builder()
                .code(content)
                .language(syntaxLang)
                .theme(if (isDark) SyntaxThemes.monokai() else SyntaxThemes.atom(false))
                .build()
        }
    }

    val visualTransformation = remember(highlights) {
        HighlightsVisualTransformation(highlights)
    }

    Box(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier.fillMaxSize(),
            label = { Text("${language.displayName} Editor") },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            visualTransformation = visualTransformation,
            maxLines = Int.MAX_VALUE,
        )
    }
}

@Composable
private fun LockboxGatekeeperPane(
    document: Scratch,
    onUnlock: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.scratch_lockbox_gatekeeper_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.scratch_lockbox_gatekeeper_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onUnlock,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.scratch_lockbox_unlock_cta))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Re-locks automatically on app restart or exit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

