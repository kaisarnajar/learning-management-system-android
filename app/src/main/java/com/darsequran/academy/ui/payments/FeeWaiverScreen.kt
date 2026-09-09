package com.darsequran.academy.ui.payments

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darsequran.academy.data.model.CouponRequestDto
import com.darsequran.academy.ui.theme.EmeraldDark
import com.darsequran.academy.ui.theme.EmeraldPrimary
import com.darsequran.academy.ui.theme.GoldAccent
import com.darsequran.academy.ui.theme.GoldDark

val WAIVER_REASON_OPTIONS = listOf(
    "Financial hardship",
    "Student / Currently studying",
    "Unemployed / Job seeking",
    "Single parent / Primary caregiver",
    "Orphan / Loss of family breadwinner",
    "Medical expenses / Health condition",
    "Disaster / Emergency relief",
    "Madrasa / Full-time Islamic student",
    "Revert / New Muslim support",
    "Senior citizen / Retired with fixed income",
    "Other"
)

@Composable
fun FeeWaiverScreen(
    viewModel: FeeWaiverViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Bar & Action CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fee Waiver Requests",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Track and manage your fee waiver requests.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.openRequestModal() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = "Request Waiver",
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Request Fee Waiver", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Guidelines Banner
                    item {
                        WaiverGuidelinesBanner()
                    }

                    // Section Title: Past Requests
                    item {
                        Text(
                            text = "Past Requests",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    if (uiState.requests.isEmpty()) {
                        item {
                            EmptyWaiverRequestsCard()
                        }
                    } else {
                        items(uiState.requests) { request ->
                            WaiverRequestCard(
                                request = request,
                                courseTitle = request.course?.title ?: uiState.courseTitlesMap[request.courseId] ?: request.courseId
                            )
                        }
                    }
                }
            }
        }
    }

    // Submit Waiver Modal Dialog
    if (uiState.showRequestModal) {
        WaiverRequestModalDialog(
            availableCourses = uiState.availableCourses,
            isSubmitting = uiState.isSubmitting,
            onDismiss = { viewModel.closeRequestModal() },
            onSubmit = { courseId, feeType, reasonCat, customReason ->
                viewModel.submitWaiverRequest(courseId, feeType, reasonCat, customReason)
            }
        )
    }
}

@Composable
fun WaiverGuidelinesBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Handshake,
                    contentDescription = "Handshake",
                    tint = GoldAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Financial Aid & Fee Waiver Program",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Darse Quran Academy provides 100% Zakat & Sadaqah funded full and partial fee waivers for eligible students. Applications are reviewed confidentially by our committee.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.88f),
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
fun EmptyWaiverRequestsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(EmeraldPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ConfirmationNumber,
                    contentDescription = "Ticket",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Fee Waiver Requests",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When you submit a fee waiver request during course enrollment or payment, your submitted requests and approved coupon status will appear here.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
fun WaiverRequestCard(
    request: CouponRequestDto,
    courseTitle: String
) {
    val statusUpper = request.status.uppercase()
    val isApproved = statusUpper == "APPROVED"
    val isPending = statusUpper == "PENDING"
    val isRejected = statusUpper in listOf("REJECTED", "DECLINED")

    val feeTypeLabel = remember(request.reason) {
        when {
            request.reason.contains("[Fee Type: Enrollment Fee]") -> "Enrollment Fee"
            request.reason.contains("[Fee Type: Course Fee]") -> "Course Fee"
            else -> "Course Fee"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = courseTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Type: $feeTypeLabel",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    )
                }

                // Status Badge Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        isApproved -> Color(0xFFDCFCE7)
                        isPending -> Color(0xFFFEF9C3)
                        else -> Color(0xFFFEE2E2)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isApproved -> Icons.Default.CheckCircle
                                isPending -> Icons.Default.HourglassEmpty
                                else -> Icons.Default.Cancel
                            },
                            contentDescription = "Status",
                            tint = when {
                                isApproved -> Color(0xFF15803D)
                                isPending -> Color(0xFFA16207)
                                else -> Color(0xFFB91C1C)
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isApproved -> "Approved"
                                isPending -> "Pending Review"
                                else -> "Rejected"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isApproved -> Color(0xFF15803D)
                                    isPending -> Color(0xFFA16207)
                                    else -> Color(0xFFB91C1C)
                                }
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reason Text
            Text(
                text = request.reason,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    fontSize = 12.5.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Assigned Coupon Code display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (request.coupon != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Assigned Coupon: ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = request.coupon.code,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = " (${request.coupon.percentage}% OFF)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        )
                    }
                } else {
                    Text(
                        text = "Assigned Coupon: —",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }

                request.createdAt?.let { dateStr ->
                    Text(
                        text = dateStr.take(10),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun WaiverRequestModalDialog(
    availableCourses: List<CourseOption>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (courseId: String, feeType: String, reasonCategory: String, customReason: String) -> Unit
) {
    var selectedCourseId by remember(availableCourses) {
        mutableStateOf(availableCourses.firstOrNull()?.id ?: "")
    }
    var courseDropdownExpanded by remember { mutableStateOf(false) }

    var selectedFeeType by remember { mutableStateOf("course") } // "course" or "enrollment"
    var feeTypeDropdownExpanded by remember { mutableStateOf(false) }

    var selectedReasonCat by remember { mutableStateOf(WAIVER_REASON_OPTIONS[0]) }
    var reasonCatDropdownExpanded by remember { mutableStateOf(false) }

    var customReason by remember { mutableStateOf("") }

    val selectedCourseTitle = remember(selectedCourseId, availableCourses) {
        availableCourses.find { it.id == selectedCourseId }?.title ?: "Select Course"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Request Fee Waiver",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "If you are unable to pay course fees, request a fee waiver below. The administration will review your request confidentialy:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Course Select
                Text(
                    text = "Course",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCourseTitle,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.clickable { courseDropdownExpanded = true }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { courseDropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = courseDropdownExpanded,
                        onDismissRequest = { courseDropdownExpanded = false }
                    ) {
                        availableCourses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course.title) },
                                onClick = {
                                    selectedCourseId = course.id
                                    courseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fee Type Select
                Text(
                    text = "Fee Type",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (selectedFeeType == "enrollment") "Enrollment Fee" else "Course Fee",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.clickable { feeTypeDropdownExpanded = true }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { feeTypeDropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = feeTypeDropdownExpanded,
                        onDismissRequest = { feeTypeDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Course Fee") },
                            onClick = {
                                selectedFeeType = "course"
                                feeTypeDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Enrollment Fee") },
                            onClick = {
                                selectedFeeType = "enrollment"
                                feeTypeDropdownExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reason Category Select
                Text(
                    text = "Reason for Waiver",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedReasonCat,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.clickable { reasonCatDropdownExpanded = true }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reasonCatDropdownExpanded = true }
                    )

                    DropdownMenu(
                        expanded = reasonCatDropdownExpanded,
                        onDismissRequest = { reasonCatDropdownExpanded = false }
                    ) {
                        WAIVER_REASON_OPTIONS.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason) },
                                onClick = {
                                    selectedReasonCat = reason
                                    reasonCatDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Additional details
                Text(
                    text = if (selectedReasonCat == "Other") "Please specify reason *" else "Additional details (optional)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = customReason,
                    onValueChange = { customReason = it },
                    placeholder = {
                        Text(if (selectedReasonCat == "Other") "Describe your reason..." else "Add any additional context...")
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedCourseId, selectedFeeType, selectedReasonCat, customReason) },
                enabled = !isSubmitting && selectedCourseId.isNotBlank() && (selectedReasonCat != "Other" || customReason.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("SUBMIT REQUEST", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = EmeraldPrimary)
            }
        }
    )
}
