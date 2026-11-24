package com.darsequran.academy.ui.courses

import kotlinx.coroutines.async
import com.darsequran.academy.data.remote.Profile
import com.darsequran.academy.data.repo.orNull
import com.darsequran.academy.ui.theme.HeroUnderStatusBar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.Session
import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.CourseDetailResponse
import com.darsequran.academy.data.remote.Enrollment
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.data.repo.AuthRepository
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.formatCalendarDate
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Avatar
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.EmptyState
import com.darsequran.academy.ui.common.ErrorState
import com.darsequran.academy.ui.common.HeroBox
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadingBox
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PagedListBody
import com.darsequran.academy.ui.common.PagedViewModel
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.SkeletonList
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.TabTopBar
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.rememberPlainText
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.ui.common.toLoad
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.theme.Brand
import com.darsequran.academy.data.remote.EnrollmentStatus
import com.darsequran.academy.ui.common.courseFeeText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// =================================================================== tab

/** The signed-in user's courses; the catalogue is a separate [PagedViewModel] (see [CoursesScreen]). */
data class CoursesState(
    val session: Session? = null,
    val mine: Load<List<Enrollment>> = Load.Loading,
    val refreshing: Boolean = false,
)

class CoursesViewModel(private val repo: AcademyRepository, private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(CoursesState())
    val state: StateFlow<CoursesState> = _state.asStateFlow()

    private var mineJob: Job? = null

    init {
        viewModelScope.launch { auth.session.collect { s -> _state.update { it.copy(session = s) } } }
        // A token refresh emits a new session for the same user: only a different user (or none) needs a reload.
        viewModelScope.launch { auth.session.map { it?.user?.id }.distinctUntilChanged().collect { loadMine() } }
    }

    /** Coming back from a course: its status may have changed. */
    fun reloadMine() { loadMine() }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        loadMine()
    }

    /** The newest load wins: one started for the previous session must not overwrite this one's courses. */
    private fun loadMine() {
        mineJob?.cancel()
        mineJob = viewModelScope.launch {
            val mine = if (auth.session.value == null) Load.Ready(emptyList()) else repo.enrollments().toLoad()
            _state.update { it.copy(mine = mine, refreshing = false) }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    onOpenCourse: (String) -> Unit,
    onOpenLearn: (Enrollment) -> Unit,
    onSignIn: () -> Unit,
    vm: CoursesViewModel = viewModel(factory = vmFactory { CoursesViewModel(it.repo, it.auth) }),
    catalogue: PagedViewModel<Course> = viewModel(key = "course-catalogue", factory = vmFactory { c -> PagedViewModel { p, q, _ -> c.repo.courses(p, q) } }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ReloadOnReturn(vm::reloadMine)
    val signedIn = state.session != null
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val showMine = signedIn && tab == 0

    Scaffold(topBar = { TabTopBar("Courses") }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (signedIn) {
                SecondaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                    Tab(tab == 0, { tab = 0 }, text = { Text("My courses") })
                    Tab(tab == 1, { tab = 1 }, text = { Text("Catalogue") })
                }
            }
            if (showMine) PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
                MyCoursesList(state.mine, onOpenCourse, onOpenLearn, vm::refresh) { tab = 1 }
            } else {
                PagedListBody(catalogue, searchHint = "Search courses", emptyTitle = "No courses found", key = { it.id }) {
                    CourseCard(it) { onOpenCourse(it.id) }
                }
            }
        }
    }
}

@Composable
private fun MyCoursesList(
    mine: Load<List<Enrollment>>,
    onOpenCourse: (String) -> Unit,
    onOpenLearn: (Enrollment) -> Unit,
    onRetry: () -> Unit,
    onBrowse: () -> Unit,
) {
    when (mine) {
        Load.Loading -> SkeletonList()
        is Load.Failed -> ErrorState(mine.message, onRetry)
        is Load.Ready -> if (mine.value.isEmpty()) {
            EmptyState("No courses yet", "Find a course in the catalogue and apply. It will appear here.", "Browse the catalogue", onBrowse)
        } else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(mine.value, key = { it.id }) { e ->
                val (label, tone) = statusLabel(e.status)
                val open = EnrollmentStatus.opensLearn(e.status)
                AppCard(Modifier.fillMaxWidth(), onClick = { if (open) onOpenLearn(e) else onOpenCourse(e.courseId) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(label, tone)
                        Spacer(Modifier.weight(1f))
                        e.rollNumber?.let {
                            Text(rollNumberLabel(it), style = MaterialTheme.typography.labelMedium, color = Brand.gold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(e.course?.title ?: "Course", style = MaterialTheme.typography.titleLarge)
                    e.course?.teacher?.let {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(it.name, it.imageUrl, 28.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(it.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    enrollmentNotes(e).forEachIndexed { i, note ->
                        Spacer(Modifier.height(6.dp))
                        val refusal = i == 0 && e.status == EnrollmentStatus.REJECTED // the website shows it in red
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (refusal) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseCard(course: Course, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        HeroBox(Modifier.fillMaxWidth().height(70.dp)) {
            Row(Modifier.padding(horizontal = 16.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Overline(course.category.ifBlank { "Course" }, color = Brand.goldSoft)
                Spacer(Modifier.weight(1f))
                if (course.level.isNotBlank()) Text(course.level, style = MaterialTheme.typography.labelMedium, color = Brand.mint)
            }
        }
        Column(Modifier.padding(16.dp)) {
            Text(course.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(rememberPlainText(course.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            course.teacher?.let {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(it.name, it.imageUrl, 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(it.name, style = MaterialTheme.typography.titleSmall)
                        Text(it.specialization, style = MaterialTheme.typography.bodySmall, color = Brand.gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                FeeBlock("Enrollment", if (course.priceInrPaise > 0) rupees(course.priceInrPaise) else "Free", Modifier.weight(1f))
                FeeBlock("Course fee", courseFeeText(course), Modifier.weight(1.4f))
            }
        }
    }
}

@Composable
private fun FeeBlock(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

// ================================================================ detail

class CourseDetailViewModel(private val repo: AcademyRepository, private val auth: AuthRepository, val courseId: String) : ViewModel() {
    private val _state = MutableStateFlow<Load<CourseDetailResponse>>(Load.Loading)
    val state: StateFlow<Load<CourseDetailResponse>> = _state.asStateFlow()
    val session get() = auth.session
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** The signed-in user's profile: whether it is complete and the e-mail verified decide the enrol button. */
    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
        // Enrolled or not, and what to pay, depend on who is signed in: signing in from "Sign in to apply" must show it.
        viewModelScope.launch { auth.session.map { it?.user?.id }.distinctUntilChanged().drop(1).collect { load() } }
    }

    /** Reloads in place (the page stays up meanwhile); the newest load wins. */
    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val me = async { if (auth.session.value != null) auth.profile().orNull() else null }
            _state.value = repo.course(courseId).toLoad()
            _profile.value = me.await()
        }
    }

    /** "Verify your email to enroll": sends the verification e-mail again. */
    fun resendVerification() {
        val email = _profile.value?.email ?: return
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                _message.value = when (val r = auth.resendVerification(email)) {
                    is ApiResult.Ok -> "$VERIFICATION_SENT $email. Open the link in it, then come back to enroll."
                    is ApiResult.Err -> r.message
                }
            } finally { _busy.value = false }
        }
    }

    /** Where the screen goes after an application, once it has been answered. */
    enum class AfterApply { PAY, COMPLETE_PROFILE }

    private val _next = MutableStateFlow<AfterApply?>(null)

    /**
     * Set when the answer to an application sends the student on; the screen goes there and calls [consumeNext].
     * State rather than a callback: the phone may have rotated while the application was sent, and the screen that
     * asked (and its navigation) is gone by then.
     */
    val next: StateFlow<AfterApply?> = _next.asStateFlow()

    fun consumeNext() { _next.value = null }

    /**
     * Applies; free courses wait for approval, paid ones continue to the enrollment-fee payment.
     * One application per tap: a second tap (even in the same frame) is ignored while the first is sent.
     */
    fun apply() {
        if (_busy.value) return
        _busy.value = true
        _message.value = null
        viewModelScope.launch { try { sendApplication() } finally { _busy.value = false } }
    }

    private suspend fun sendApplication() {
        when (val r = repo.enroll(courseId)) {
            is ApiResult.Ok -> {
                load()
                if (r.value.requiresEnrollmentFee == true) _next.value = AfterApply.PAY
                else _message.value = ENROLL_REQUESTED
            }
            is ApiResult.Err -> {
                if (r.message.contains("complete your profile", ignoreCase = true)) _next.value = AfterApply.COMPLETE_PROFILE
                _message.value = r.message
            }
        }
    }
}

@Composable
fun CourseDetailScreen(
    courseId: String,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onPay: (type: String, title: String) -> Unit,
    onOpenLearn: (String) -> Unit,
    onOpenTeacher: (String) -> Unit,
    onCompleteProfile: () -> Unit,
    vm: CourseDetailViewModel = viewModel(key = courseId, factory = vmFactory { CourseDetailViewModel(it.repo, it.auth, courseId) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    // Back from paying the enrollment fee (or anything else): the enrollment may have moved on, e.g. to "under review",
    // and the page must not keep offering the payment just made.
    ReloadOnReturn(vm::load)
    val next by vm.next.collectAsStateWithLifecycle()
    LaunchedEffect(next, state) {
        val title = (state as? Load.Ready)?.value?.course?.title ?: return@LaunchedEffect
        when (next) {
            CourseDetailViewModel.AfterApply.PAY -> onPay("enrollment", title)
            CourseDetailViewModel.AfterApply.COMPLETE_PROFILE -> onCompleteProfile()
            null -> return@LaunchedEffect
        }
        vm.consumeNext()
    }

    when (val s = state) {
        Load.Loading -> Column { HeaderBack(onBack); LoadingBox() }
        is Load.Failed -> Column { HeaderBack(onBack); ErrorState(s.message, vm::load) }
        is Load.Ready -> {
            val detail = s.value
            val course = detail.course
            Scaffold(
                bottomBar = {
                    CourseActionBar(
                        detail, enrollGate(session != null, profile), busy,
                        EnrollActions(
                            signIn = onSignIn,
                            apply = vm::apply,
                            pay = { onPay("enrollment", course.title) },
                            open = { onOpenLearn(course.title) },
                            completeProfile = onCompleteProfile,
                            verifyEmail = vm::resendVerification,
                        ),
                    )
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                Column(Modifier.padding(bottom = padding.calculateBottomPadding()).verticalScroll(rememberScrollState())) {
                    HeroUnderStatusBar()
                    HeroBox(Modifier.fillMaxWidth()) {
                        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(bottom = 24.dp)) {
                            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Brand.onHero) }
                            Column(Modifier.padding(horizontal = 20.dp)) {
                                Overline(course.category.ifBlank { "Course" }, color = Brand.goldSoft)
                                Spacer(Modifier.height(6.dp))
                                Text(course.title, style = MaterialTheme.typography.headlineMedium, color = Brand.onHero)
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (course.level.isNotBlank()) StatusChip(course.level, Tone.Success)
                                    detail.enrollment?.let { val (l, t) = statusLabel(it.status); StatusChip(l, t) }
                                }
                            }
                        }
                    }
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        message?.let {
                            val good = it == ENROLL_REQUESTED || it.startsWith(VERIFICATION_SENT)
                            Banner(it, if (good) Tone.Success else Tone.Warning)
                        }
                        if (!detail.enrollmentOpen && detail.enrollment == null) {
                            Banner(enrollmentClosedMessage(course.status), Tone.Warning)
                        }

                        AppCard(Modifier.fillMaxWidth()) {
                            InfoRow(Icons.Rounded.CalendarMonth, "Starts", course.startDate.ifBlank { null }?.let(::formatCalendarDate) ?: "To be announced")
                            InfoRow(Icons.Rounded.Schedule, "Duration", course.duration.ifBlank { "—" })
                            InfoRow(Icons.Rounded.SignalCellularAlt, "Level", course.level.ifBlank { "All levels" })
                            InfoRow(Icons.Rounded.Payments, "Enrollment fee", if (course.priceInrPaise > 0) rupees(course.priceInrPaise) else "Free")
                            InfoRow(Icons.Rounded.Payments, "Course fee", courseFeeText(course), last = true)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                FEES_NOTE, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        course.teacher?.let { t ->
                            AppCard(Modifier.fillMaxWidth(), onClick = { onOpenTeacher(t.id) }) {
                                Overline("Your teacher")
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(t.name, t.imageUrl, 52.dp)
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text(t.name, style = MaterialTheme.typography.titleLarge)
                                        Text(t.specialization, style = MaterialTheme.typography.bodySmall, color = Brand.gold)
                                    }
                                }
                            }
                        }

                        Column {
                            Text("About this course", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Text(rememberPlainText(course.description).ifBlank { "Details will be shared by the academy." }, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBack(onBack: () -> Unit) {
    Box(Modifier.windowInsetsPadding(WindowInsets.statusBars)) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Brand.gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(Brand.outlineSoft))
}

private const val ENROLL_REQUESTED = "Enrollment requested. The academy will review it soon."
private const val VERIFICATION_SENT = "Verification e-mail sent to"
private const val FEES_NOTE = "Fees are set by the academy for each course. Paid courses require an enrollment fee " +
    "before access is granted; monthly fees are paid from your profile after enrollment is active."

/** The website's message for a course that takes no new students (getCourseEnrollmentClosedMessage). */
internal fun enrollmentClosedMessage(status: String): String = when (status) {
    "DRAFT" -> "This course is not available."
    "ON_HOLD" -> "Enrollment is temporarily on hold for this course."
    "COMPLETED" -> "This course has completed. New enrollments are closed."
    "COMING_SOON" -> "Enrollment has not started yet. Coming soon!"
    else -> "Enrollment is not available for this course."
}

/** What stands between this person and requesting enrollment, checked in the website's order. */
internal enum class EnrollGate { SignIn, CompleteProfile, VerifyEmail, Ready }

/** The profile still loading counts as ready: the server checks again when the request is sent. */
internal fun enrollGate(signedIn: Boolean, profile: Profile?): EnrollGate = when {
    !signedIn -> EnrollGate.SignIn
    profile == null -> EnrollGate.Ready
    !profile.profileComplete -> EnrollGate.CompleteProfile
    profile.emailVerified == null -> EnrollGate.VerifyEmail
    else -> EnrollGate.Ready
}

internal class EnrollActions(
    val signIn: () -> Unit,
    val apply: () -> Unit,
    val pay: () -> Unit,
    val open: () -> Unit,
    val completeProfile: () -> Unit,
    val verifyEmail: () -> Unit,
)

/** The enrol button's label and whether it acts: the website's CourseEnrollButton, state by state. */
internal fun enrollButton(
    detail: CourseDetailResponse,
    gate: EnrollGate,
    actions: EnrollActions,
): Triple<String, () -> Unit, Boolean> {
    // A rejected request behaves like none: the student may request again.
    val enrollment = detail.enrollment?.takeIf { it.status != "rejected" }
    return when {
        enrollment?.status == "completed" -> Triple("Completed — Open course", actions.open, true)
        enrollment?.status == "active" -> Triple("Enrolled — Open course", actions.open, true)
        enrollment?.status == "pending_approval" -> Triple("Awaiting enrollment approval", {}, false)
        enrollment?.status == "awaiting_enrollment_fee" ->
            if (enrollment.enrollmentFeePending) Triple("Awaiting payment verification", {}, false)
            else Triple("Pay enrollment fee · ${rupees(detail.course.priceInrPaise)}", actions.pay, true)
        enrollment != null -> Triple("Open course", actions.open, true)
        !detail.enrollmentOpen -> Triple("Not accepting students", {}, false)
        gate == EnrollGate.SignIn -> Triple("Sign in to request enrollment", actions.signIn, true)
        gate == EnrollGate.CompleteProfile -> Triple("Complete profile to enroll", actions.completeProfile, true)
        gate == EnrollGate.VerifyEmail -> Triple("Verify your email to enroll", actions.verifyEmail, true)
        else -> Triple("Request enrollment", actions.apply, true)
    }
}

/** The single next step for this person and this course. */
@Composable
private fun CourseActionBar(detail: CourseDetailResponse, gate: EnrollGate, busy: Boolean, actions: EnrollActions) {
    val (label, action, enabled) = enrollButton(detail, gate, actions)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton(label, action, enabled = enabled, loading = busy)
        }
    }
}
