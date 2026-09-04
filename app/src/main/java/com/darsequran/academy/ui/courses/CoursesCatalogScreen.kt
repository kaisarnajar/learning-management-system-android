package com.darsequran.academy.ui.courses

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darsequran.academy.data.model.CourseDto
import com.darsequran.academy.ui.components.CompactSearchBar
import com.darsequran.academy.ui.theme.EmeraldPrimary
import com.darsequran.academy.ui.theme.GoldAccent
import com.darsequran.academy.ui.theme.GoldDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesCatalogScreen(
    viewModel: CoursesCatalogViewModel,
    onBackPress: () -> Unit = {},
    onTeacherClick: (String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.enrollmentSuccessMessage, uiState.errorMessage) {
        uiState.enrollmentSuccessMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
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
            // Compact Search Bar
            CompactSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                placeholderText = "Search by title, category, or teacher..."
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Courses Feed Area
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.filteredCourses.isEmpty()) {
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
                            text = "No matching courses found",
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
                    items(uiState.filteredCourses) { course ->
                        PublicCourseCard(
                            course = course,
                            onViewDetails = { viewModel.selectCourseDetail(course) },
                            onRequestEnrollment = { viewModel.requestEnrollment(course.id) },
                            isEnrolling = uiState.isEnrolling,
                            onTeacherClick = { teacherName ->
                                viewModel.selectTeacherDetailByName(teacherName, course.teacher?.specialization)
                            }
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

    // Detail Bottom Sheet Modal
    uiState.selectedCourseDetail?.let { course ->
        CourseDetailBottomSheet(
            course = course,
            isEnrolling = uiState.isEnrolling,
            onDismissRequest = { viewModel.selectCourseDetail(null) },
            onRequestEnrollment = { courseId -> viewModel.requestEnrollment(courseId) },
            onTeacherClick = { teacherName, specialization ->
                viewModel.selectCourseDetail(null)
                viewModel.selectTeacherDetailByName(teacherName, specialization)
            }
        )
    }

    // Teacher Detail Bottom Sheet Modal
    uiState.selectedTeacherDetail?.let { teacher ->
        com.darsequran.academy.ui.teachers.TeacherDetailBottomSheet(
            teacher = teacher,
            courses = uiState.courses,
            onCourseClick = { course -> viewModel.selectCourseDetail(course) },
            onDismissRequest = { viewModel.selectTeacherDetail(null) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailBottomSheet(
    course: CourseDto,
    isEnrolling: Boolean,
    onDismissRequest: () -> Unit,
    onRequestEnrollment: (String) -> Unit,
    onTeacherClick: (String, String?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Badges Row + Share Icon
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
                        text = (course.category ?: "ISLAMIC STUDIES").uppercase(),
                        bgColor = GoldAccent.copy(alpha = 0.18f),
                        textColor = GoldDark
                    )
                    CourseBadge(
                        text = (course.level ?: "Beginner"),
                        bgColor = Color(0xFFEFEBE9),
                        textColor = Color(0xFF5D4037)
                    )
                    CourseBadge(
                        text = (course.status ?: "Published"),
                        bgColor = Color(0xFFEDE7F6),
                        textColor = Color(0xFF512DA8)
                    )
                }

                val context = LocalContext.current
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, course.title)
                            putExtra(Intent.EXTRA_TEXT, "Check out the course: ${course.title} at Darse Quran Academy!\nhttps://staging-learning-management-system-teal-ten.vercel.app/courses/${course.id}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Course"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Course Title
            Text(
                text = course.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Meta Info
            Text(
                text = "Starts: ${course.startDate ?: "Ongoing"}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
            Text(
                text = "Duration: ${course.duration ?: "Self-paced"}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Course Description (Dynamic API Driven)
            if (!course.description.isNullOrBlank()) {
                Text(
                    text = course.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Instructor Card
            val teacherName = course.teacher?.name ?: "Moulana Abdul Rahman"
            val instructorSubtitle = course.teacher?.specialization ?: course.title
            InstructorCard(
                teacherName = teacherName,
                subtitle = instructorSubtitle,
                onClick = {
                    onTeacherClick(teacherName, instructorSubtitle)
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Fees Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FEES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldDark,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val regFee = course.displayEnrollmentFee
                    val regFeeText = if (regFee > 0) "₹$regFee" else "Free"
                    SpecRow(label = "Enrollment:", value = regFeeText)
                    Spacer(modifier = Modifier.height(6.dp))

                    val monthlyFee = course.displayMonthlyFee
                    val cycle = course.displayFeeFrequency
                    SpecRow(label = "Fee:", value = "₹$monthlyFee / month ($cycle)")

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Fees are set by the academy for each course. Paid courses require an enrollment fee before access is granted; monthly fees are paid from your profile after enrollment is active.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            lineHeight = 18.sp,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Syllabus & Learning Outcomes (API Driven)
            if (!course.syllabus.isNullOrBlank()) {
                Text(
                    text = "Course Curriculum & Syllabus",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = course.syllabus,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (!course.learningOutcomes.isNullOrBlank()) {
                Text(
                    text = "What You'll Learn",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = course.learningOutcomes,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Request / Pay Enrollment Button
            Button(
                onClick = { onRequestEnrollment(course.id) },
                enabled = !isEnrolling,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldDark,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isEnrolling) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    val buttonText = if (course.displayEnrollmentFee > 0) "Pay enrollment fee" else "Request enrollment"
                    Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PublicCourseCard(
    course: CourseDto,
    onViewDetails: () -> Unit,
    onRequestEnrollment: () -> Unit,
    isEnrolling: Boolean,
    onTeacherClick: (String?) -> Unit
) {
    val teacherName = course.teacher?.name ?: "Moulana Abdul Rahman"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Badges Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CourseBadge(
                    text = (course.category ?: "ISLAMIC STUDIES").uppercase(),
                    bgColor = GoldAccent.copy(alpha = 0.18f),
                    textColor = GoldDark
                )
                CourseBadge(
                    text = (course.level ?: "Beginner"),
                    bgColor = Color(0xFFEFEBE9),
                    textColor = Color(0xFF5D4037)
                )
                CourseBadge(
                    text = (course.status ?: "Published"),
                    bgColor = Color(0xFFEDE7F6),
                    textColor = Color(0xFF512DA8)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Course Title
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 19.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description Paragraph
            course.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        lineHeight = 20.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Instructor Card (Clickable to open Teacher Detail bottom sheet!)
            InstructorCard(
                teacherName = teacherName,
                subtitle = course.teacher?.specialization,
                onClick = { onTeacherClick(teacherName) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Course Specs Summary
            Column(modifier = Modifier.fillMaxWidth()) {
                SpecRow(label = "Starts:", value = course.startDate ?: "Ongoing")
                Spacer(modifier = Modifier.height(4.dp))
                SpecRow(label = "Duration:", value = course.duration ?: "8 weeks")
                Spacer(modifier = Modifier.height(4.dp))
                val regFee = course.registrationFee?.toInt() ?: 0
                SpecRow(label = "Enrollment:", value = "₹$regFee")
                Spacer(modifier = Modifier.height(4.dp))
                val monthlyFee = course.fee?.toInt() ?: 349
                val cycle = course.billingCycle ?: "Monthly"
                SpecRow(label = "Fee:", value = "₹$monthlyFee / month ($cycle)")
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Button 1: COURSE DETAILS (Outlined)
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, GoldDark)
                ) {
                    Text(
                        text = "COURSE DETAILS",
                        fontWeight = FontWeight.Bold,
                        color = GoldDark,
                        fontSize = 13.5.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Button 2: Request enrollment (Solid Gold)
                Button(
                    onClick = onRequestEnrollment,
                    enabled = !isEnrolling,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldDark,
                        contentColor = Color.White
                    )
                ) {
                    if (isEnrolling) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Request enrollment",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstructorCard(
    teacherName: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "INSTRUCTOR",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = GoldDark,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar Circle
                val initials = teacherName.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()
                    .ifEmpty { "AR" }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(EmeraldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teacherName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                    )
                    subtitle?.let { sub ->
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = GoldDark,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Instructor",
                    tint = GoldDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CourseBadge(
    text: String,
    bgColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 10.5.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 13.5.sp
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.5.sp
            )
        )
    }
}

@Composable
fun SyllabusModuleItem(
    moduleNumber: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Check",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$moduleNumber: $title",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 13.5.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 17.sp,
                    fontSize = 12.sp
                )
            )
        }
    }
}
