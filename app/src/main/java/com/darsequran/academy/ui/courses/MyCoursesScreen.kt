package com.darsequran.academy.ui.courses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darsequran.academy.data.model.AttendanceSummaryDto
import com.darsequran.academy.data.model.EnrollmentDto
import com.darsequran.academy.ui.theme.EmeraldDark
import com.darsequran.academy.ui.theme.GoldAccent
import com.darsequran.academy.ui.theme.GoldDark

@Composable
fun MyCoursesScreen(
    viewModel: MyCoursesViewModel,
    onViewAllCourses: () -> Unit = {},
    onPayFee: (EnrollmentDto) -> Unit = {},
    onOpenCourse: (EnrollmentDto) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Section matching Web UI Spec
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "My Enrolled Courses",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                val studentName = uiState.userName ?: "Kaisar Ahmad Najar"
                Text(
                    text = "Welcome back, $studentName. Enrolled programs, attendance, and fee payments appear below.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onViewAllCourses,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldDark,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "EXPLORE CATALOG",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Explore",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.enrollments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "No courses",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You have not enrolled in any courses yet.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.enrollments) { enrollment ->
                        val attSummary = enrollment.course?.let { uiState.attendanceSummaries[it.id] }
                        EnrolledCourseCard(
                            enrollment = enrollment,
                            attendanceSummary = attSummary,
                            onPayFee = { onPayFee(enrollment) },
                            onOpenCourse = { onOpenCourse(enrollment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnrolledCourseCard(
    enrollment: EnrollmentDto,
    attendanceSummary: AttendanceSummaryDto? = null,
    onPayFee: () -> Unit = {},
    onOpenCourse: () -> Unit = {}
) {
    val course = enrollment.course
    val statusLower = enrollment.status.lowercase()

    val (statusLabel, statusBg, statusTextColor, isFeeAction, isApprovalAction) = when {
        statusLower.contains("fee") || statusLower.contains("payment") || statusLower.contains("awaiting_payment") -> {
            Tuple5("Awaiting Fee Payment", Color(0xFFFEF9C3), Color(0xFFA16207), true, false)
        }
        statusLower.contains("pending") || statusLower.contains("approval") -> {
            Tuple5("Pending Approval", Color(0xFFFEF9C3), Color(0xFFA16207), false, true)
        }
        else -> {
            Tuple5("Enrolled", Color(0xFFDCFCE7), Color(0xFF15803D), false, false)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Badges Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CourseBadge(
                        text = (course?.category ?: "ISLAMIC STUDIES").uppercase(),
                        bgColor = GoldAccent.copy(alpha = 0.18f),
                        textColor = GoldDark
                    )
                    CourseBadge(
                        text = (course?.level ?: "Beginner"),
                        bgColor = Color(0xFFEFEBE9),
                        textColor = Color(0xFF5D4037)
                    )
                    enrollment.rollNumber?.let { roll ->
                        CourseBadge(
                            text = "Roll: $roll",
                            bgColor = Color(0xFFE0F2FE),
                            textColor = Color(0xFF0369A1)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor,
                            fontSize = 11.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Course Title
            Text(
                text = course?.title ?: "Tajweed-ul-Quran & Recitation Course",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 19.sp
                )
            )

            // Course Description
            course?.description?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Structured Specifications Box (Web Spec)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SpecRow(label = "Starts:", value = course?.startDate ?: "Ongoing")
                    Spacer(modifier = Modifier.height(4.dp))
                    SpecRow(label = "Duration:", value = course?.duration ?: "Self-paced")
                    Spacer(modifier = Modifier.height(4.dp))
                    course?.teacher?.name?.let { tName ->
                        val spec = course.teacher.specialization
                        val tText = if (!spec.isNullOrBlank()) "$tName ($spec)" else tName
                        SpecRow(label = "Instructor:", value = tText)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    val monthlyFee = course?.displayMonthlyFee ?: 0
                    val cycle = course?.displayFeeFrequency ?: "Monthly"
                    SpecRow(label = "Fee:", value = "₹$monthlyFee / month ($cycle)")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons / Status CTA Container
            when {
                isFeeAction -> {
                    Button(
                        onClick = onPayFee,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "PAY ENROLLMENT FEE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                isApprovalAction -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Pending",
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Awaiting Academy Approval",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = onOpenCourse,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldDark,
                            contentColor = Color.White
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ENTER CLASSROOM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Enter",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
