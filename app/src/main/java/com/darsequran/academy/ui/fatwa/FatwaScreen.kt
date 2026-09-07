package com.darsequran.academy.ui.fatwa

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darsequran.academy.data.model.FatwaItemDto
import com.darsequran.academy.ui.theme.EmeraldDark
import com.darsequran.academy.ui.theme.GoldAccent
import com.darsequran.academy.ui.theme.GoldDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FatwaScreen(
    viewModel: FatwaViewModel,
    onBackPress: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    uiState.submitSuccessMessage?.let { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleAskFatwaDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.AddComment, contentDescription = "Ask Fatwa", tint = GoldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask Question", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                // Compact Search Bar
                com.darsequran.academy.ui.components.CompactSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    placeholderText = "Search Fiqh rulings or questions..."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Content Area
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.filteredFatwas.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "No fatwas",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No matching Fiqh Q&As found",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.filteredFatwas) { fatwa ->
                            FatwaCard(
                                fatwa = fatwa,
                                onClick = { viewModel.selectFatwaDetail(fatwa) }
                            )
                        }
                        item {
                            com.darsequran.academy.ui.components.PaginationBar(
                                currentPage = uiState.currentPage,
                                totalPages = uiState.totalPages,
                                totalCount = uiState.totalCount,
                                onPageSelected = { viewModel.onPageSelected(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Fatwa Reading Modal Sheet (Matching Web UI Fatwa Detail View)
    uiState.selectedFatwaDetail?.let { fatwa ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectFatwaDetail(null) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Gold Accent Bar
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(GoldDark)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Large Uppercase Title (Centered)
                Text(
                    text = fatwa.title.uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 21.sp,
                        lineHeight = 27.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Category Subtitle (Centered)
                Text(
                    text = "Category: ${fatwa.category.uppercase()}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Question Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Question",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = GoldDark,
                                fontSize = 17.sp
                            )
                        )

                        val askedBy = fatwa.scholarName ?: fatwa.askerName ?: "DARUL IFTA-DQA"
                        val answeredDate = fatwa.answeredAt ?: "28 June 2026"
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Asked by $askedBy · Answered $answeredDate",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                fontSize = 13.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = fatwa.question,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                fontSize = 14.5.sp,
                                lineHeight = 22.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Answer Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Answer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = GoldDark,
                                fontSize = 17.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = fatwa.answer ?: "Question pending scholar review.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                                fontSize = 14.5.sp,
                                lineHeight = 23.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Submit Fatwa / Ask Question Sheet (Matching Web UI Form Spec)
    if (uiState.isAskFatwaDialogOpen) {
        AskQuestionSheet(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { viewModel.toggleAskFatwaDialog(false) },
            onSubmit = { subject, question, category, name, email ->
                viewModel.submitFatwaQuestion(subject, question, category, name, email)
            }
        )
    }
}

@Composable
fun FatwaCard(
    fatwa: FatwaItemDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 1. Top Row: Category Badge + Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GoldAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = fatwa.category.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldDark,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                fatwa.answeredAt?.let { date ->
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            fontSize = 13.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Question Title
            Text(
                text = fatwa.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    lineHeight = 25.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 3. Question Snippet Body
            if (fatwa.question.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = fatwa.question,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Asked By Metadata
            val askedBy = fatwa.scholarName ?: fatwa.askerName ?: "DARUL IFTA-DQA"
            Text(
                text = "Asked by $askedBy",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontSize = 13.5.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 5. "Read answer →" Action Link
            Text(
                text = "Read answer →",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GoldDark,
                    fontSize = 14.5.sp
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskQuestionSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, question: String, category: String, name: String, email: String) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var questionDetail by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("Kaisar Ahmad Najar") }
    var email by remember { mutableStateOf("kaisarnajar11114@gmail.com") }

    val categories = listOf(
        "Islam",
        "Quran",
        "Hadith",
        "Fiqh",
        "Tajweed",
        "Seerah",
        "Arabic Language",
        "Atheism",
        "Fatwa",
        "Others"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ask a Question",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Category Dropdown Field
            Row {
                Text(
                    text = "Category ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "*",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Red
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            var dropdownExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (category.isBlank()) "Select a topic" else category,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = GoldDark
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (category.isBlank()) Color.Gray else MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = GoldDark,
                        disabledContainerColor = Color.Transparent
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { dropdownExpanded = true }
                )

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.88f)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Select a topic",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                            )
                        },
                        onClick = {
                            category = ""
                            dropdownExpanded = false
                        }
                    )
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (category == cat) FontWeight.Bold else FontWeight.Normal,
                                        color = if (category == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            },
                            onClick = {
                                category = cat
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Subject Field
            Row {
                Text(
                    text = "Subject ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "*",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Red
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                placeholder = { Text("Brief summary of your question", color = Color.Gray, fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldDark,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Your Question Field
            Row {
                Text(
                    text = "Your question ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "*",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Red
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = questionDetail,
                onValueChange = { questionDetail = it },
                placeholder = {
                    Text(
                        "Describe your question in detail (e.g. Fiqh, Quran, Tajweed, daily practice)",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldDark,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Send Question Anonymously Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAnonymous = !isAnonymous }
            ) {
                Checkbox(
                    checked = isAnonymous,
                    onCheckedChange = { isAnonymous = it },
                    colors = CheckboxDefaults.colors(checkedColor = GoldDark)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Send Question Anonymously",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Helper text
            val helperText = if (isAnonymous) {
                "Submitting anonymously. We will email you when answered."
            } else {
                "Submitting as $name ($email). We will email you when answered."
            }
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Submit Question Button
            val isFormValid = category.isNotBlank() && subject.isNotBlank() && questionDetail.isNotBlank()
            Button(
                onClick = {
                    if (isFormValid) {
                        val submitName = if (isAnonymous) "Anonymous" else name
                        onSubmit(subject, questionDetail, category, submitName, email)
                    }
                },
                enabled = isFormValid && !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldDark,
                    disabledContainerColor = GoldDark.copy(alpha = 0.4f),
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = "Submit question",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
