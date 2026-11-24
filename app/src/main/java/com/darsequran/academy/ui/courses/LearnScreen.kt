package com.darsequran.academy.ui.courses

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.remote.CourseAnnouncement
import com.darsequran.academy.data.remote.CourseAnnouncementsResponse
import com.darsequran.academy.data.remote.CourseAttendance
import com.darsequran.academy.data.remote.CourseGrades
import com.darsequran.academy.data.remote.GradeEntry
import com.darsequran.academy.data.remote.PaymentHistory
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.announcementCategory
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.EmptyState
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadContent
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.downloadAndOpen
import com.darsequran.academy.ui.common.formatCalendarDate
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.relativeTime
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.common.StatusKind
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.ui.common.toLoad
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.theme.Brand
import com.darsequran.academy.data.remote.Enrollment
import com.darsequran.academy.data.remote.PaymentSubmission
import com.darsequran.academy.data.repo.orNull
import com.darsequran.academy.data.remote.EnrollmentStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LearnViewModel(private val repo: AcademyRepository, val courseId: String) : ViewModel() {
    private val _updates = MutableStateFlow<Load<CourseAnnouncementsResponse>>(Load.Loading)
    val updates: StateFlow<Load<CourseAnnouncementsResponse>> = _updates.asStateFlow()
    private val _attendance = MutableStateFlow<Load<CourseAttendance?>>(Load.Loading)
    val attendance: StateFlow<Load<CourseAttendance?>> = _attendance.asStateFlow()
    private val _grades = MutableStateFlow<Load<CourseGrades?>>(Load.Loading)
    val grades: StateFlow<Load<CourseGrades?>> = _grades.asStateFlow()
    private val _fees = MutableStateFlow<Load<PaymentHistory>>(Load.Loading)
    val fees: StateFlow<Load<PaymentHistory>> = _fees.asStateFlow()
    /** This course's enrollment (status, fee), which decides whether a course fee can be paid from here. */
    private val _enrollment = MutableStateFlow<Enrollment?>(null)
    val enrollment: StateFlow<Enrollment?> = _enrollment.asStateFlow()

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch { _updates.value = repo.courseAnnouncements(courseId).toLoad() }
        viewModelScope.launch { _attendance.value = repo.attendance(courseId).toLoad() }
        viewModelScope.launch { _grades.value = repo.grades(courseId).toLoad() }
        viewModelScope.launch { _fees.value = repo.paymentHistory().toLoad() }
        viewModelScope.launch { repo.enrollments().orNull()?.let { list -> _enrollment.value = list.firstOrNull { it.courseId == courseId } } }
    }
}

@Composable
fun LearnScreen(
    courseId: String,
    title: String,
    onBack: () -> Unit,
    onPayFee: () -> Unit,
    onCourseInfo: () -> Unit,
    vm: LearnViewModel = viewModel(key = "learn-$courseId", factory = vmFactory { LearnViewModel(it.repo, courseId) }),
) {
    ReloadOnReturn(vm::loadAll)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    Scaffold(
        topBar = { BackTopBar(title, onBack, actions = { androidx.compose.material3.TextButton(onCourseInfo) { Text("Details") } }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp, containerColor = MaterialTheme.colorScheme.background) {
                listOf("Announcements", "Attendance", "Grades", "Fees").forEachIndexed { i, label ->
                    Tab(tab == i, { tab = i }, text = { Text(label) })
                }
            }
            when (tab) {
                0 -> UpdatesTab(vm, snackbar)
                1 -> AttendanceTab(vm)
                2 -> GradesTab(vm)
                else -> FeesTab(vm, onPayFee)
            }
        }
    }
}

@Suppress("FunctionNaming") // Compose names composables like types
@Composable
private fun UpdatesTab(vm: LearnViewModel, snackbar: SnackbarHostState) {
    val state by vm.updates.collectAsStateWithLifecycle()
    LoadContent(state, vm::loadAll) { data ->
        if (data.courseWide.isEmpty() && data.personal.isEmpty()) {
            EmptyState("No announcements yet. Check back later.")
        } else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (data.personal.isNotEmpty()) {
                item { SectionHeading("For you", "Private messages from your instructor") }
                items(data.personal, key = { "p" + it.id }) { AnnouncementItem(it, personal = true, snackbar) }
                item { Spacer(Modifier.height(8.dp)) }
            }
            if (data.courseWide.isNotEmpty()) {
                item { SectionHeading("Course-wide", "Visible to everyone in this course") }
                items(data.courseWide, key = { "c" + it.id }) { AnnouncementItem(it, personal = false, snackbar) }
            }
        }
    }
}

@Suppress("FunctionNaming") // Compose names composables like types
@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AnnouncementItem(item: CourseAnnouncement, personal: Boolean, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AppCard(Modifier.fillMaxWidth(), color = if (personal) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (personal) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(14.dp), tint = Brand.gold)
                Spacer(Modifier.width(6.dp))
                Text("For you only", style = MaterialTheme.typography.labelMedium, color = Brand.gold)
                Text(" · ", style = MaterialTheme.typography.labelMedium, color = Brand.gold)
            }
            Text(item.authorName.ifBlank { "Academy" }, style = MaterialTheme.typography.labelMedium, color = Brand.gold, modifier = Modifier.weight(1f))
            Text(relativeTime(item.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Text(item.title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(item.body, style = MaterialTheme.typography.bodyMedium)
        item.category?.let { Spacer(Modifier.height(8.dp)); StatusChip(announcementCategory(it), Tone.Neutral) }
        if (item.attachmentPath != null) {
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = {
                    scope.launch {
                        snackbar.showSnackbar("Opening ${item.attachmentName ?: "attachment"}…")
                        downloadAndOpen(context, item.attachmentPath, item.attachmentName ?: "attachment")?.let { snackbar.showSnackbar(it) }
                    }
                },
                label = { Text(item.attachmentName ?: "Attachment") },
                leadingIcon = { Icon(Icons.Rounded.AttachFile, null, Modifier.size(18.dp)) },
            )
        }
    }
}

@Suppress("FunctionNaming") // Compose names composables like types
@Composable
private fun AttendanceTab(vm: LearnViewModel) {
    val state by vm.attendance.collectAsStateWithLifecycle()
    LoadContent(state, vm::loadAll) { data ->
        if (data == null || data.totalClasses == 0) {
            EmptyState("No attendance records found yet.")
        } else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                SummaryCard(
                    "Your Attendance", "Review your presence across all conducted sessions.",
                    listOf(
                        "Sessions" to "${data.totalClasses}",
                        "Present" to "${data.presentClasses}",
                        "Absent" to "${data.absentClasses}",
                        "Rate" to "${data.percentage}%",
                    ),
                )
            }
            items(data.records.sortedByDescending { it.date }, key = { it.id }) { r ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(formatCalendarDate(r.date), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    StatusChip(if (r.isPresent) "Present" else "Absent", if (r.isPresent) Tone.Success else Tone.Danger)
                }
            }
        }
    }
}

/** A website report header: a title, a line under it, and its figures (label over value). */
@Suppress("FunctionNaming") // Compose names composables like types
@Composable
private fun SummaryCard(title: String, subtitle: String, figures: List<Pair<String, String>>) {
    AppCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            figures.forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = MaterialTheme.typography.titleLarge)
                    Text(
                        label.uppercase(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Suppress("FunctionNaming") // Compose names composables like types
@Composable
private fun GradesTab(vm: LearnViewModel) {
    val state by vm.grades.collectAsStateWithLifecycle()
    LoadContent(state, vm::loadAll) { data ->
        if (data == null || data.grades.isEmpty()) {
            EmptyState("No grade records found for this student yet.")
        } else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                val sum = gradeSummary(data.grades)
                SummaryCard(
                    "Grade Report", "Detailed performance summary.",
                    listOf(
                        "Exams" to "${sum.exams}",
                        "Obtained" to formatMarks(sum.obtained),
                        "Max" to formatMarks(sum.max),
                        "Percentage" to "${sum.percentage}%",
                    ),
                )
            }
            items(data.grades.sortedByDescending { it.date }, key = { it.id }) { g -> GradeRow(g) }
        }
    }
}

@Suppress("FunctionNaming") // Compose names composables like types
@Composable
private fun GradeRow(g: GradeEntry) {
    val (marks, note) = gradeMarks(g)
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(g.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    formatCalendarDate(g.date), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    marks, style = MaterialTheme.typography.titleMedium,
                    color = if (g.marksObtained == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary,
                    modifier = if (g.marksObtained == null) Modifier.semantics { contentDescription = "Not graded" }
                    else Modifier,
                )
                note?.let {
                    Text(
                        it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Whether "Pay course fee" makes sense: the student is studying the course (not finished, not waiting), it has a fee,
 * and a one-time fee isn't already paid or under review. The Pay screen's quote from the server has the final word.
 */
internal fun canPayCourseFee(enrollment: Enrollment?, submissions: List<PaymentSubmission>): Boolean {
    val course = enrollment?.course ?: return false
    if (enrollment.status != EnrollmentStatus.ACTIVE || course.monthlyFeeInrPaise <= 0) return false
    if (course.feeFrequency != "ONE_TIME") return true
    return submissions.none { it.courseId == enrollment.courseId && it.paymentType != "enrollment" && it.status != "rejected" && it.status != "declined" }
}


@Composable
private fun FeesTab(vm: LearnViewModel, onPayFee: () -> Unit) {
    val state by vm.fees.collectAsStateWithLifecycle()
    val enrollment by vm.enrollment.collectAsStateWithLifecycle()
    LoadContent(state, vm::loadAll) { history ->
        val submissions = history.submissions.filter { it.courseId == vm.courseId }.sortedByDescending { it.createdAt }
        val canPay = canPayCourseFee(enrollment, submissions)
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (canPay) item { PrimaryButton("Pay course fee", onPayFee); Spacer(Modifier.height(8.dp)) }
            if (submissions.isEmpty()) item { EmptyState("No payments yet", "Payments you submit for this course are listed here.") }
            items(submissions, key = { it.id }) { s ->
                val (label, tone) = statusLabel(s.status, StatusKind.Payment)
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.label, style = MaterialTheme.typography.titleMedium)
                            Text(formatDate(s.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(rupees(s.amountInrPaise), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            StatusChip(label, tone)
                        }
                    }
                }
            }
        }
    }
}

/** Circular progress with a label in the middle. */
@Composable
fun ProgressRing(fraction: Float, label: String, size: Int = 76) {
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val color = MaterialTheme.colorScheme.primary
    val gold = Brand.goldSoft
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
            drawArc(if (fraction >= 0.75f) color else gold, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
