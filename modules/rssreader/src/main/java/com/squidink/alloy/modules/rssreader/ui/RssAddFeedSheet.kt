package com.squidink.alloy.modules.rssreader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.squidink.alloy.modules.rssreader.data.network.GoogleNewsCategory
import com.squidink.alloy.modules.rssreader.data.network.GoogleNewsLanguage
import com.squidink.alloy.modules.rssreader.data.network.GoogleNewsRecency
import com.squidink.alloy.modules.rssreader.data.network.GoogleNewsUrlBuilder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RssAddFeedSheet(
    onDismiss: () -> Unit,
    onAddFeed: (url: String, title: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Google News tab state
    var selectedCategory by remember { mutableStateOf(GoogleNewsCategory.TECHNOLOGY) }
    var selectedLanguage by remember { mutableStateOf(GoogleNewsLanguage.US) }
    var selectedRecency by remember { mutableStateOf(GoogleNewsRecency.ANY) }
    var customQuery by remember { mutableStateOf("") }
    var expandedLanguage by remember { mutableStateOf(false) }
    var expandedRecency by remember { mutableStateOf(false) }

    // Custom URL tab state
    var customUrl by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Add RSS Subscription",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Google News") },
                    icon = { Icon(Icons.Default.Public, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Custom RSS") },
                    icon = { Icon(Icons.Default.RssFeed, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // ==================== Google News Tab ====================
                Text(
                    text = "Category / Topic",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GoogleNewsCategory.entries.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.displayName) }
                        )
                    }
                }

                // Keywords Search
                OutlinedTextField(
                    value = customQuery,
                    onValueChange = { customQuery = it },
                    label = { Text("Keywords / Topic Filter (Optional)") },
                    placeholder = { Text("e.g. Android Kotlin, AI, Semiconductors") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                // Recency Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedRecency,
                    onExpandedChange = { expandedRecency = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = "Recency: ${selectedRecency.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRecency) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRecency,
                        onDismissRequest = { expandedRecency = false }
                    ) {
                        GoogleNewsRecency.entries.forEach { recency ->
                            DropdownMenuItem(
                                text = { Text(recency.displayName) },
                                onClick = {
                                    selectedRecency = recency
                                    expandedRecency = false
                                }
                            )
                        }
                    }
                }

                // Language & Country Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedLanguage,
                    onExpandedChange = { expandedLanguage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    OutlinedTextField(
                        value = "Region: ${selectedLanguage.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedLanguage,
                        onDismissRequest = { expandedLanguage = false }
                    ) {
                        GoogleNewsLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.displayName) },
                                onClick = {
                                    selectedLanguage = lang
                                    expandedLanguage = false
                                }
                            )
                        }
                    }
                }

                // Live Preview Card
                val previewTitle = GoogleNewsUrlBuilder.buildTitle(
                    category = selectedCategory,
                    query = customQuery,
                    recency = selectedRecency,
                    language = selectedLanguage
                )
                val previewUrl = GoogleNewsUrlBuilder.buildUrl(
                    category = selectedCategory,
                    query = customQuery,
                    recency = selectedRecency,
                    language = selectedLanguage
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Feed Preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = previewTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = previewUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        onAddFeed(previewUrl, previewTitle)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Subscribe to Google News Feed")
                }

            } else {
                // ==================== Custom RSS Tab ====================
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { Text("Feed URL") },
                    placeholder = { Text("https://example.com/feed.xml") },
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { customUrl = it.trim() }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Custom Title (Optional)") },
                    placeholder = { Text("Leave blank to use channel title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (customUrl.isNotBlank()) {
                            onAddFeed(customUrl.trim(), customTitle.trim().ifEmpty { null })
                            onDismiss()
                        }
                    },
                    enabled = customUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Subscribe to Feed")
                }
            }
        }
    }
}
