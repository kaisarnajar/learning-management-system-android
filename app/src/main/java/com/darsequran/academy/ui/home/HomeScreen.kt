package com.darsequran.academy.ui.home

import com.darsequran.academy.ui.account.ResendState
import com.darsequran.academy.ui.account.resendLabel
import com.darsequran.academy.ui.account.resendOutcome
import com.darsequran.academy.ui.common.ctaButtonColors
import com.darsequran.academy.ui.theme.HeroUnderStatusBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.R
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.Session
import com.darsequran.academy.data.remote.BlogPost
import com.darsequran.academy.data.remote.Book
import com.darsequran.academy.data.remote.Course
import com.darsequran.academy.data.remote.Enrollment
import com.darsequran.academy.data.remote.Fatwa
import com.darsequran.academy.data.remote.HomeResponse
import com.darsequran.academy.data.remote.Inspiration
import com.darsequran.academy.data.remote.SiteAnnouncement
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.data.repo.AuthRepository
import com.darsequran.academy.data.repo.orNull
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.ArabicText
import com.darsequran.academy.ui.common.Avatar
import com.darsequran.academy.ui.common.ErrorState
import com.darsequran.academy.ui.common.HeroBox
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PriceLine
import com.darsequran.academy.ui.common.RemoteImage
import com.darsequran.academy.ui.common.SectionHeader
import com.darsequran.academy.ui.common.SkeletonList
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.feeFrequencySuffix
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.rememberPlainText
import com.darsequran.academy.ui.common.relativeTime
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.theme.Brand
import com.darsequran.academy.ui.common.academyToday
import com.darsequran.academy.ui.common.feePeriod
import com.darsequran.academy.ui.common.monthlyFeeDue
import com.darsequran.academy.data.remote.EnrollmentStatus
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.TextButton
import com.darsequran.academy.data.remote.LibraryItem
import com.darsequran.academy.data.remote.Review
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Something the student should do now, shown at the top of Home. */
data class ActionCard(val kind: Kind, val title: String, val message: String, val courseId: String? = null, val courseTitle: String? = null) {
    enum class Kind { PayEnrollment, PayCourseFee, UnderReview, CompleteProfile, VerifyEmail }
}

data class HomeState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val content: HomeResponse? = null,
    val session: Session? = null,
    val enrollments: List<Enrollment> = emptyList(),
    val actions: List<ActionCard> = emptyList(),
    val unread: Int = 0,
    val resend: ResendState = ResendState(),
)

class HomeViewModel(private val repo: AcademyRepository, private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeState(content = repo.cachedHome(), loading = repo.cachedHome() == null))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch { auth.session.collect { s -> _state.update { it.copy(session = s) } } }
        // A token refresh emits a new session for the same user: only a different user (or none) needs a reload.
        viewModelScope.launch { auth.session.map { it?.user?.id }.distinctUntilChanged().collect { load() } }
    }

    private var loadJob: Job? = null

    fun refresh() = load(refreshing = true)

    /** Sends the verification link again; once sent, the button stays "Sent" (as on the website). */
    fun resendVerification() {
        val email = auth.session.value?.user?.email ?: return
        val current = _state.value.resend
        if (current.sending || current.sent) return
        _state.update { it.copy(resend = ResendState(sending = true)) }
        viewModelScope.launch {
            val result = auth.resendVerification(email)
            _state.update { it.copy(resend = resendOutcome(result)) }
        }
    }
    fun reload() = load()

    /** The newest load wins: a slower one started for the previous session must not overwrite it (or its user's data). */
    private fun load(refreshing: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadNow(refreshing) }
    }

    private suspend fun loadNow(refreshing: Boolean) = coroutineScope {
        _state.update { it.copy(refreshing = refreshing, error = null) }
        val signedIn = auth.session.value != null
        val home = async { repo.home() }
        val enrollments = async { if (signedIn) repo.enrollments().orNull() ?: emptyList() else emptyList() }
        val history = async { if (signedIn) repo.paymentHistory().orNull() else null }
        val profile = async { if (signedIn) auth.profile().orNull() else null }
        val unread = async { if (signedIn) repo.notifications(1, unreadOnly = true).orNull()?.unreadCount ?: 0 else 0 }

        val homeResult = home.await()
        val list = enrollments.await()
        val submissions = history.await()?.submissions.orEmpty()
        val me = profile.await()

        val actions = buildList {
            if (me != null && me.emailVerified == null) add(ActionCard(ActionCard.Kind.VerifyEmail, "Verify your e-mail", "Check your inbox for the link. You need it to enrol and pay."))
            if (me != null && !me.profileComplete) add(ActionCard(ActionCard.Kind.CompleteProfile, "Complete your profile", "Your details are needed before you can enrol in a course."))
            val pending = submissions.filter { it.status == "pending_verification" }
            for (e in list) {
                val course = e.course ?: continue
                val pendingHere = pending.filter { it.courseId == e.courseId }
                if (e.status == "awaiting_enrollment_fee" && pendingHere.none { it.paymentType == "enrollment" }) {
                    add(ActionCard(ActionCard.Kind.PayEnrollment, "Pay the enrollment fee", "${course.title} · ${rupees(course.priceInrPaise)}", course.id, course.title))
                }
                if (monthlyFeeDue(e, submissions)) {
                    add(ActionCard(ActionCard.Kind.PayCourseFee, "${feePeriod(academyToday())} fee is due", "${course.title} · ${rupees(course.monthlyFeeInrPaise)}", course.id, course.title))
                }
            }
            if (pending.isNotEmpty()) add(ActionCard(ActionCard.Kind.UnderReview, "Payment under review", if (pending.size == 1) "The academy will confirm it shortly." else "${pending.size} payments are being verified."))
        }

        _state.update {
            it.copy(
                loading = false,
                refreshing = false,
                content = (homeResult as? ApiResult.Ok)?.value ?: it.content,
                error = (homeResult as? ApiResult.Err)?.message?.takeIf { _ -> it.content == null },
                enrollments = list,
                actions = actions,
                unread = unread.await(),
            )
        }
    }
}

// Baselined for its size before the library and testimonials callbacks changed its signature.
@Suppress("FunctionNaming", "LongMethod", "CyclomaticComplexMethod")
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCourse: (String) -> Unit,
    onOpenLearn: (Enrollment) -> Unit,
    onPay: (courseId: String, type: String, title: String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBlogPost: (String) -> Unit,
    onOpenFatwa: (String) -> Unit,
    onOpenBook: (String) -> Unit,
    onOpenAnnouncement: (String) -> Unit,
    onSeeCourses: () -> Unit,
    onSeeExplore: () -> Unit,
    onSeeBookstore: () -> Unit,
    onSignIn: () -> Unit,
    onEditProfile: () -> Unit,
    onPayments: () -> Unit,
    onOpenLibraryItem: (String) -> Unit,
    onSeeLibrary: () -> Unit,
    onWriteReview: () -> Unit,
    vm: HomeViewModel = viewModel(factory = vmFactory { HomeViewModel(it.repo, it.auth) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    ReloadOnReturn(vm::reload)

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { HomeHero(state.session, state.unread, state.content?.dailyInspiration, onOpenNotifications, onSignIn) }

            val content = state.content
            if (content == null) {
                item { if (state.error != null) ErrorState(state.error!!, vm::refresh) else SkeletonList(4) }
                return@LazyColumn
            }

            if (state.actions.isNotEmpty()) {
                item { Spacer(Modifier.height(20.dp)) }
                items(state.actions) { action ->
                    ActionCardView(action, Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                        when (action.kind) {
                            ActionCard.Kind.PayEnrollment -> onPay(action.courseId!!, "enrollment", action.courseTitle!!)
                            ActionCard.Kind.PayCourseFee -> onPay(action.courseId!!, "course", action.courseTitle!!)
                            ActionCard.Kind.UnderReview -> onPayments()
                            ActionCard.Kind.CompleteProfile -> onEditProfile()
                            ActionCard.Kind.VerifyEmail -> onEditProfile()
                        }
                    }
                    if (action.kind == ActionCard.Kind.VerifyEmail) {
                        VerifyEmailResend(state.resend, vm::resendVerification)
                    }
                }
            }

            if (state.session != null && state.enrollments.isNotEmpty()) {
                item { Spacer(Modifier.height(24.dp)); SectionHeader("My courses", action = "All", onAction = onSeeCourses) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.enrollments, key = { it.id }) { e -> MyCourseCard(e) { if (EnrollmentStatus.opensLearn(e.status)) onOpenLearn(e) else onOpenCourse(e.courseId) } }
                    }
                }
            } else if (state.session == null) {
                item { JoinCard(onSignIn, onSeeCourses) }
            }

            if (content.announcements.isNotEmpty()) {
                item { Spacer(Modifier.height(20.dp)); SectionHeader("Announcements", action = "All", onAction = onSeeExplore) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(content.announcements, key = { it.id }) { AnnouncementCard(it) { onOpenAnnouncement(it.id) } }
                    }
                }
            }

            if (content.courses.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)); SectionHeader("Featured courses", action = "Catalogue", onAction = onSeeCourses) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(content.courses, key = { it.id }) { CourseTile(it) { onOpenCourse(it.id) } }
                    }
                }
            }

            if (content.blogPosts.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)); SectionHeader("From the blog", action = "More", onAction = onSeeExplore) }
                items(content.blogPosts.take(3), key = { "b" + it.id }) { post ->
                    BlogRow(post, Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { onOpenBlogPost(post.id) }
                }
            }

            if (content.fatwas.isNotEmpty()) {
                item { Spacer(Modifier.height(18.dp)); SectionHeader("Recent fatwas", action = "More", onAction = onSeeExplore) }
                items(content.fatwas.take(3), key = { "f" + it.id }) { fatwa ->
                    FatwaRow(fatwa, Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { onOpenFatwa(fatwa.id) }
                }
            }

            if (content.libraryItems.isNotEmpty()) libraryItems(content.libraryItems, onOpenLibraryItem, onSeeLibrary)

            if (content.books.isNotEmpty()) {
                item { Spacer(Modifier.height(18.dp)); SectionHeader("Bookstore", action = "Shop", onAction = onSeeBookstore) }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(content.books, key = { it.id }) { BookTile(it) { onOpenBook(it.id) } }
                    }
                }
            }

            testimonials(content.reviews, onWriteReview)
        }
    }
}

// ---------------------------------------------------------------- parts

@Composable
private fun HomeHero(session: Session?, unread: Int, inspiration: Inspiration?, onNotifications: () -> Unit, onSignIn: () -> Unit) {
    HeroUnderStatusBar()
    HeroBox(Modifier.fillMaxWidth()) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.logo), "Darse Quran Academy", Modifier.size(40.dp).clip(CircleShape).background(Color.White))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Assalamu alaikum", style = MaterialTheme.typography.bodySmall, color = Brand.mint)
                    Text(
                        session?.user?.name?.split(" ")?.firstOrNull() ?: "Darse Quran Academy",
                        style = MaterialTheme.typography.titleLarge,
                        color = Brand.onHero,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (session != null) {
                    IconButton(onClick = onNotifications) {
                        BadgedBox(badge = { if (unread > 0) Badge(containerColor = Brand.goldSoft, contentColor = Color.Black) { Text(if (unread > 9) "9+" else "$unread") } }) {
                            Icon(Icons.Rounded.Notifications, "Notifications", tint = Brand.onHero)
                        }
                    }
                } else {
                    Text(
                        "Sign in",
                        style = MaterialTheme.typography.labelLarge,
                        color = Brand.goldSoft,
                        modifier = Modifier.clip(CircleShape).clickable(onClick = onSignIn).padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            if (inspiration != null) {
                Spacer(Modifier.height(18.dp))
                InspirationCard(inspiration, Modifier.padding(end = 12.dp))
            }
        }
    }
}

@Composable
fun InspirationCard(inspiration: Inspiration, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier.fillMaxWidth().clip(shape).background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Brand.goldSoft.copy(alpha = 0.55f), shape).padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Overline(if (inspiration.kind == "HADITH") "Hadith of the day" else "Verse of the day", color = Brand.goldSoft)
        // Amiri's marks rise well above the cap height; leave them room.
        Spacer(Modifier.height(18.dp))
        ArabicText(inspiration.arabicText, color = Brand.onHero, size = 24)
        Spacer(Modifier.height(6.dp))
        Text(
            inspiration.englishTranslation,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, fontFamily = MaterialTheme.typography.titleLarge.fontFamily),
            color = Brand.onHero.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
        )
        inspiration.reference?.let {
            Spacer(Modifier.height(8.dp))
            Text("— $it", style = MaterialTheme.typography.labelMedium, color = Brand.goldSoft)
        }
    }
}

@Composable
private fun ActionCardView(action: ActionCard, modifier: Modifier, onClick: () -> Unit) {
    val (icon, tint, bg) = when (action.kind) {
        ActionCard.Kind.PayEnrollment, ActionCard.Kind.PayCourseFee -> Triple(Icons.Rounded.Payments, Brand.warning, Brand.warningContainer)
        ActionCard.Kind.UnderReview -> Triple(Icons.Rounded.HourglassTop, Brand.info, Brand.infoContainer)
        ActionCard.Kind.CompleteProfile -> Triple(Icons.Rounded.Badge, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        ActionCard.Kind.VerifyEmail -> Triple(Icons.Rounded.MarkEmailUnread, Brand.danger, Brand.dangerContainer)
    }
    AppCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(action.title, style = MaterialTheme.typography.titleMedium)
                Text(action.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            ActionIcon(Icons.AutoMirrored.Rounded.ArrowForward)
        }
    }
}

@Composable
private fun ActionIcon(icon: ImageVector) = Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))

@Composable
private fun JoinCard(onSignIn: () -> Unit, onBrowse: () -> Unit) {
    AppCard(Modifier.fillMaxWidth().padding(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.School, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Learn with the academy", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Quran, Tajweed, Fiqh and more with qualified teachers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.Button(
                onClick = onSignIn, modifier = Modifier.weight(1f), colors = ctaButtonColors(),
            ) { Text("Sign in") }
            androidx.compose.material3.OutlinedButton(onClick = onBrowse, modifier = Modifier.weight(1f)) { Text("Browse courses") }
        }
    }
}

@Composable
private fun MyCourseCard(enrollment: Enrollment, onClick: () -> Unit) {
    val course = enrollment.course
    val (label, tone) = statusLabel(enrollment.status)
    AppCard(Modifier.width(250.dp), onClick = onClick) {
        StatusChip(label, tone)
        Spacer(Modifier.height(10.dp))
        Text(course?.title ?: "Course", style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(44.dp))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(course?.teacher?.name, course?.teacher?.imageUrl, 26.dp)
            Spacer(Modifier.width(8.dp))
            Text(course?.teacher?.name ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
            enrollment.rollNumber?.let { Text("Roll $it", style = MaterialTheme.typography.labelMedium, color = Brand.gold) }
        }
    }
}

@Composable
private fun AnnouncementCard(item: SiteAnnouncement, onClick: () -> Unit) {
    AppCard(Modifier.width(280.dp), onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        if (item.images.isNotEmpty()) {
            RemoteImage(item.images.first().imagePath, null, Modifier.fillMaxWidth().height(120.dp))
        }
        Column(Modifier.padding(14.dp)) {
            Overline(item.eventDate?.let { formatDate(it) } ?: relativeTime(item.createdAt))
            Spacer(Modifier.height(4.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(rememberPlainText(item.body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CourseTile(course: Course, onClick: () -> Unit) {
    AppCard(Modifier.width(240.dp), onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        HeroBox(Modifier.fillMaxWidth().height(86.dp)) {
            Column(Modifier.padding(14.dp)) {
                Overline(course.category.ifBlank { "Course" }, color = Brand.goldSoft)
                Spacer(Modifier.height(4.dp))
                Text(course.level, style = MaterialTheme.typography.labelMedium, color = Brand.mint)
            }
        }
        Column(Modifier.padding(14.dp)) {
            Text(course.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(44.dp))
            Spacer(Modifier.height(6.dp))
            Text(course.teacher?.name ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(if (course.monthlyFeeInrPaise > 0) rupees(course.monthlyFeeInrPaise) else "Free", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (course.monthlyFeeInrPaise > 0) {
                    Spacer(Modifier.width(4.dp))
                    Text(feeFrequencySuffix(course.feeFrequency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun BlogRow(post: BlogPost, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row {
            RemoteImage(post.images.firstOrNull()?.imagePath, null, Modifier.size(76.dp).clip(MaterialTheme.shapes.medium))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(post.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(rememberPlainText(post.excerpt ?: post.body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(listOfNotNull(post.createdBy?.name, formatDate(post.createdAt)).joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = Brand.gold)
            }
        }
    }
}

@Composable
fun FatwaRow(fatwa: Fatwa, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppCard(modifier.fillMaxWidth(), onClick = onClick) {
        Overline(fatwa.category)
        Spacer(Modifier.height(4.dp))
        Text(fatwa.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        Text(rememberPlainText(fatwa.answer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun BookTile(book: Book, modifier: Modifier = Modifier.width(150.dp), onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick)) {
        RemoteImage(book.imagePath, book.title, Modifier.fillMaxWidth().aspectRatio(0.72f).clip(MaterialTheme.shapes.medium))
        Spacer(Modifier.height(8.dp))
        Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        PriceLine(book.priceInrPaise, book.mrpInrPaise)
    }
}

// ------------------------------------------------- library and testimonials (the website's homepage sections)

private const val CARD_IMAGE_RATIO = 16f / 9f
private const val MAX_STARS = 5

/** The website's FeaturedResources: "Study Materials" from the digital library. */
private fun LazyListScope.libraryItems(items: List<LibraryItem>, onOpen: (String) -> Unit, onSeeAll: () -> Unit) {
    item {
        Spacer(Modifier.height(18.dp))
        SectionHeader("Study Materials", action = "View all", onAction = onSeeAll)
        SectionIntro("Books and study materials from the academy digital library.")
    }
    item {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { "l" + it.id }) { LibraryCard(it) { onOpen(it.id) } }
        }
    }
}

/** The website's StudentTestimonials: approved reviews, or an invitation to write the first. */
private fun LazyListScope.testimonials(reviews: List<Review>, onWriteReview: () -> Unit) {
    item {
        Spacer(Modifier.height(18.dp))
        SectionHeader("What our students say", action = "Write Review", onAction = onWriteReview)
        SectionIntro(
            "Hear from learners across Jammu & Kashmir and beyond who study Quran, Tajweed, and Islamic " +
                "sciences with us online.",
        )
    }
    if (reviews.isEmpty()) {
        item {
            Text(
                "Be the first to share your experience — your review may appear here after admin approval.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
        return
    }
    item {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(reviews, key = { "r" + it.id }) { TestimonialCard(it) }
        }
    }
}

@Suppress("FunctionNaming") // a composable, named like one
@Composable
private fun SectionIntro(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
    )
}

@Suppress("FunctionNaming") // a composable, named like one
@Composable
private fun LibraryCard(item: LibraryItem, onClick: () -> Unit) {
    AppCard(Modifier.width(260.dp), onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        if (item.imagePath != null) {
            RemoteImage(item.imagePath, item.title, Modifier.fillMaxWidth().aspectRatio(CARD_IMAGE_RATIO))
        }
        Column(Modifier.padding(14.dp)) {
            if (item.topic.isNotBlank()) Overline(item.topic)
            Spacer(Modifier.height(4.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.author.isNotBlank()) {
                Text(
                    "by ${item.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val facts = listOf(item.level, item.language).filter { it.isNotBlank() }
            if (facts.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(facts.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = Brand.gold)
            }
        }
    }
}

@Suppress("FunctionNaming") // a composable, named like one
@Composable
private fun TestimonialCard(review: Review) {
    AppCard(Modifier.width(280.dp)) {
        Row(Modifier.align(Alignment.CenterHorizontally)) {
            repeat(MAX_STARS) { i ->
                Icon(
                    if (i < review.rating) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    null,
                    tint = if (i < review.rating) Brand.gold else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "“${review.quote}”",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(review.name, null)
            Spacer(Modifier.height(8.dp))
            review.name?.let { Text(it, style = MaterialTheme.typography.titleSmall) }
            review.location?.let {
                val muted = MaterialTheme.colorScheme.onSurfaceVariant
                Text(it, style = MaterialTheme.typography.labelSmall, color = muted)
            }
            review.course?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Brand.gold) }
        }
    }
}

/** The website's "Resend Verification Email" button, under the verify-email card, with its outcome. */
@Suppress("FunctionNaming") // a composable, named like one
@Composable
private fun VerifyEmailResend(state: ResendState, onResend: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        TextButton(onResend, enabled = !state.sending && !state.sent) { Text(resendLabel(state)) }
        state.message?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = if (state.sent) Brand.success else Brand.danger,
                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
            )
        }
    }
}
