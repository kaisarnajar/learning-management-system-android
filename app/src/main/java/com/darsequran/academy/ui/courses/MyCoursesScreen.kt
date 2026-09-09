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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.darsequran.academy.data.model.EnrollmentDto

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
            // Header Section matching Desktop UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "My Courses",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Welcome, ${uiState.userName ?: "Kaisar Ahmad Najar"}. Enrolled programs and monthly fee payments appear below.",
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
                        containerColor = Color(0xFFD4A017)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "View All Courses",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
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
                        EnrolledCourseCard(
                            enrollment = enrollment,
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
    onPayFee: () -> Unit = {},
    onOpenCourse: () -> Unit = {}
) {
    val course = enrollment.course
    val statusLower = enrollment.status.lowercase()

    val (statusLabel, statusBg, statusTextColor, isFeeAction, isApprovalAction) = when {
        statusLower.contains("fee") || statusLower.contains("payment") || statusLower.contains("awaiting_payment") -> {
            Tuple5("Awaiting enrollment fee", Color(0xFFFEF9C3), Color(0xFFA16207), true, false)
        }
        statusLower.contains("pending") || statusLower.contains("approval") -> {
            Tuple5("Awaiting approval", Color(0xFFFEF9C3), Color(0xFFA16207), false, true)
        }
        else -> {
            Tuple5(
                enrollment.status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                Color(0xFFDCFCE7),
                Color(0xFF15803D),
                false,
                false
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Status Badge Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusBg
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = statusTextColor,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Course Title
            Text(
                text = course?.title ?: "Tajweed-ul-Quran & Recitation Course",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            )

            // Course Description
            course?.description?.let { desc ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Starts Spec
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Starts: ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFC68A16),
                        fontSize = 13.5.sp
                    )
                )
                Text(
                    text = course?.startDate ?: "Ongoing",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        fontSize = 13.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Duration Spec
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Duration: ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFC68A16),
                        fontSize = 13.5.sp
                    )
                )
                Text(
                    text = course?.duration ?: "Self-paced",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        fontSize = 13.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Row / Button
            when {
                isFeeAction -> {
                    OutlinedButton(
                        onClick = onPayFee,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFEFCE8),
                            contentColor = Color(0xFF9A3412)
                        )
                    ) {
                        Text(
                            text = "Pay enrollment fee",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                isApprovalAction -> {
                    Text(
                        text = "Awaiting enrollment approval by the academy.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9A3412),
                            fontSize = 13.5.sp
                        )
                    )
                }
                else -> {
                    OutlinedButton(
                        onClick = onOpenCourse,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFFC68A16)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFEFCE8),
                            contentColor = Color(0xFF9A3412)
                        )
                    ) {
                        Text(
                            text = "Go to Classroom",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
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
