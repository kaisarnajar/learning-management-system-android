package com.darsequran.academy.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.outlined.CastForEducation
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.rounded.CastForEducation
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.runtime.key
import com.darsequran.academy.BuildConfig
import com.darsequran.academy.data.AppEnvironment
import com.darsequran.academy.ui.common.EnvironmentStrip
import com.darsequran.academy.ui.manage.AdminDashboardScreen
import com.darsequran.academy.ui.manage.AdminDashboardTab
import com.darsequran.academy.ui.manage.TeachTab
import com.darsequran.academy.ui.manage.manageGraph
import com.darsequran.academy.ui.manage.routeForAdminHref
import com.darsequran.academy.ui.teach.TeachHomeScreen
import com.darsequran.academy.ui.theme.HeroUnderStatusBar
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.darsequran.academy.data.remote.AppConfig
import com.darsequran.academy.data.repo.orNull
import com.darsequran.academy.ui.account.AboutScreen
import com.darsequran.academy.ui.account.AccountScreen
import com.darsequran.academy.ui.account.ChangePasswordScreen
import com.darsequran.academy.ui.account.ContactScreen
import com.darsequran.academy.ui.account.DeleteAccountScreen
import com.darsequran.academy.ui.account.EditProfileScreen
import com.darsequran.academy.ui.account.NotificationsScreen
import com.darsequran.academy.ui.account.PaymentsScreen
import com.darsequran.academy.ui.account.ReviewsScreen
import com.darsequran.academy.ui.account.WaiversScreen
import com.darsequran.academy.ui.auth.ForgotPasswordScreen
import com.darsequran.academy.ui.auth.GoogleWelcomeEffect
import com.darsequran.academy.ui.auth.RegisterScreen
import com.darsequran.academy.ui.auth.SignInScreen
import com.darsequran.academy.ui.bookstore.BookScreen
import com.darsequran.academy.ui.bookstore.BookstoreScreen
import com.darsequran.academy.ui.bookstore.CartScreen
import com.darsequran.academy.ui.bookstore.CheckoutScreen
import com.darsequran.academy.ui.bookstore.OrderScreen
import com.darsequran.academy.ui.bookstore.OrdersScreen
import com.darsequran.academy.ui.courses.CourseDetailScreen
import com.darsequran.academy.ui.courses.CoursesScreen
import com.darsequran.academy.ui.courses.LearnScreen
import com.darsequran.academy.ui.explore.AnnouncementScreen
import com.darsequran.academy.ui.explore.AnnouncementsScreen
import com.darsequran.academy.ui.explore.AskFatwaScreen
import com.darsequran.academy.ui.explore.BlogListScreen
import com.darsequran.academy.ui.explore.BlogPostScreen
import com.darsequran.academy.ui.explore.ExploreScreen
import com.darsequran.academy.ui.explore.FatwaDetailScreen
import com.darsequran.academy.ui.explore.FatwaListScreen
import com.darsequran.academy.ui.explore.LibraryItemScreen
import com.darsequran.academy.ui.explore.LibraryListScreen
import com.darsequran.academy.ui.explore.TeacherScreen
import com.darsequran.academy.ui.explore.TeachersScreen
import com.darsequran.academy.ui.home.HomeScreen
import com.darsequran.academy.ui.pay.PayScreen
import com.darsequran.academy.ui.common.LinkTarget
import com.darsequran.academy.ui.common.LocalOpenLink
import com.darsequran.academy.ui.common.linkTarget
import com.darsequran.academy.ui.common.openWeb
import com.darsequran.academy.ui.common.rememberContainer
import com.darsequran.academy.data.remote.Roles
import kotlin.reflect.KClass
import kotlinx.coroutines.delay

internal data class TabItem(val route: Any, val type: KClass<*>, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val homeTab = TabItem(HomeTab, HomeTab::class, "Home", Icons.Outlined.Home, Icons.Rounded.Home)
private val coursesTab = TabItem(CoursesTab, CoursesTab::class, "Courses", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Rounded.MenuBook)
private val exploreTab = TabItem(ExploreTab, ExploreTab::class, "Explore", Icons.Outlined.Explore, Icons.Rounded.Explore)
private val accountTab = TabItem(AccountTab, AccountTab::class, "Account", Icons.Outlined.Person, Icons.Rounded.Person)

private val teachTab =
    TabItem(TeachTab, TeachTab::class, "Teach", Icons.Outlined.CastForEducation, Icons.Rounded.CastForEducation)
private fun adminTab(label: String) =
    TabItem(AdminDashboardTab, AdminDashboardTab::class, label, Icons.Outlined.Dashboard, Icons.Rounded.Dashboard)

/**
 * The tabs a signed-in user sees. As on the website, everyone gets the public app (Home, Courses, Explore, Account);
 * staff also get one entry into their area, named as in the website's header: Teach, Admin or Developer.
 */
internal enum class Shell(staff: TabItem?) {
    Student(null),
    Teacher(teachTab),
    Admin(adminTab("Admin")),
    Developer(adminTab("Developer"));

    val tabs: List<TabItem> = listOfNotNull(homeTab, coursesTab, exploreTab, staff, accountTab)
    val start: Any = HomeTab

    /** Admins and developers share the admin panel. */
    val adminPanel: Boolean get() = this == Admin || this == Developer

    companion object {
        fun of(role: String?) = when (role) {
            Roles.ADMIN -> Admin
            Roles.DEVELOPER -> Developer
            Roles.TEACHER -> Teacher
            else -> Student
        }
    }
}

private fun NavHostController.switchTab(route: Any) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val container = rememberContainer()
    val session by container.sessionStore.session.collectAsStateWithLifecycle()
    val env = AppEnvironment.current
    val shell = Shell.of(session?.user?.role)

    // A new role or user starts a fresh navigation graph: nothing from the previous one carries over.
    // The user matters even within one role: after a student signs out (or another signs in on the phone), the tab
    // stacks saved by switchTab would otherwise bring back the previous student's screens and data.
    key(shell, session?.user?.id) {
        GraphScope("${env.key}-$shell-${session?.user?.id}") {
            val nav = rememberNavController()
            Column(Modifier.fillMaxSize()) {
                if (env != AppEnvironment.PRODUCTION) {
                    HeroUnderStatusBar()
                    EnvironmentStrip(env)
                }
                Box(Modifier.weight(1f).then(if (env != AppEnvironment.PRODUCTION) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier)) {
                    AppContent(nav, shell)
                }
            }
        }
    }
}

@Composable
private fun AppContent(nav: NavHostController, shell: Shell) {
    val context = LocalContext.current
    val container = rememberContainer()
    val session by container.sessionStore.session.collectAsStateWithLifecycle()
    val tabs = shell.tabs
    var config by remember { mutableStateOf<AppConfig?>(null) }
    // Branding, lookups (e.g. fatwa categories) and policy links: keep trying until it loads, e.g. after starting offline.
    LaunchedEffect(Unit) {
        var wait = 5_000L
        while (true) {
            config = container.repo.appConfig().orNull() ?: run { delay(wait); wait = (wait * 2).coerceAtMost(60_000L); null }
            if (config != null) break
        }
    }
    // As the website's /auth/continue: a Google sign-in with an incomplete profile goes on to the profile form.
    GoogleWelcomeEffect { nav.navigate(EditProfileRoute) }

    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.type) } == true }

    /** Runs [block] when signed in, otherwise opens sign-in first. */
    fun signedIn(block: () -> Unit) { if (container.sessionStore.session.value != null) block() else nav.navigate(SignInRoute) }

    /** Website links from notifications → app screens. */
    /** Website links (notifications, article links) → app screens; false when the app has no screen for it. */
    fun openHref(href: String): Boolean {
        val destination = routeForHref(href, shell) ?: return false
        when {
            destination.isTab -> nav.switchTab(destination.route)
            // Staff pages open inside the staff tab, so Back leads to the staff area, not to wherever the link was.
            destination.underTab != null -> { nav.switchTab(destination.underTab); nav.navigate(destination.route) }
            else -> nav.navigate(destination.route)
        }
        return true
    }

    /** Links inside articles, fatwas and announcements: academy pages open here, anything else in the browser. */
    val openLink: (String) -> Unit = { href ->
        when (val target = linkTarget(href)) {
            is LinkTarget.InApp -> if (!openHref(target.path)) openWeb(context, target.path)
            is LinkTarget.Outside -> openWeb(context, target.url)
        }
    }

    CompositionLocalProvider(LocalOpenLink provides openLink) {
        Scaffold(
            bottomBar = {
                AnimatedVisibility(showBar, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        tabs.forEach { tab ->
                            val selected = destination?.hierarchy?.any { it.hasRoute(tab.type) } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { nav.switchTab(tab.route) },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.secondaryContainer),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                nav,
                startDestination = shell.start,
                modifier = Modifier.fillMaxSize().padding(bottom = if (showBar) padding.calculateBottomPadding() else 0.dp),
                enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() },
            ) {
                // ---- tabs
                composable<HomeTab>(enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    HomeScreen(
                        onOpenCourse = { nav.navigate(CourseRoute(it)) },
                        onOpenLearn = { nav.navigate(LearnRoute(it.courseId, it.course?.title ?: "Course")) },
                        onPay = { id, type, title -> nav.navigate(PayRoute(id, type, title)) },
                        onOpenNotifications = { nav.navigate(NotificationsRoute) },
                        onOpenBlogPost = { nav.navigate(BlogPostRoute(it)) },
                        onOpenFatwa = { nav.navigate(FatwaDetailRoute(it)) },
                        onOpenBook = { nav.navigate(BookRoute(it)) },
                        onOpenAnnouncement = { nav.navigate(AnnouncementRoute(it)) },
                        onSeeCourses = { nav.switchTab(CoursesTab) },
                        onSeeExplore = { nav.switchTab(ExploreTab) },
                        onSeeBookstore = { nav.navigate(BookstoreRoute) },
                        onSignIn = { nav.navigate(SignInRoute) },
                        onEditProfile = { nav.navigate(EditProfileRoute) },
                        onPayments = { nav.navigate(PaymentsRoute) },
                        onOpenLibraryItem = { nav.navigate(LibraryItemRoute(it)) },
                        onSeeLibrary = { nav.navigate(LibraryRoute) },
                        onWriteReview = { signedIn { nav.navigate(ReviewsRoute) } },
                    )
                }
                composable<CoursesTab>(enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    CoursesScreen(
                        onOpenCourse = { nav.navigate(CourseRoute(it)) },
                        onOpenLearn = { nav.navigate(LearnRoute(it.courseId, it.course?.title ?: "Course")) },
                        onSignIn = { nav.navigate(SignInRoute) },
                    )
                }
                composable<ExploreTab>(enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    ExploreScreen(
                        onBlog = { nav.navigate(BlogRoute) }, onFatwa = { nav.navigate(FatwaRoute) }, onLibrary = { nav.navigate(LibraryRoute) },
                        onAnnouncements = { nav.navigate(AnnouncementsRoute) }, onTeachers = { nav.navigate(TeachersRoute) }, onBookstore = { nav.navigate(BookstoreRoute) },
                    )
                }
                composable<AccountTab>(enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                    AccountScreen(
                        onSignIn = { nav.navigate(SignInRoute) }, onRegister = { nav.navigate(RegisterRoute) },
                        onEditProfile = { nav.navigate(EditProfileRoute) }, onPayments = { nav.navigate(PaymentsRoute) },
                        onWaivers = { nav.navigate(WaiversRoute) }, onReviews = { nav.navigate(ReviewsRoute) }, onOrders = { nav.navigate(OrdersRoute) },
                        onNotifications = { nav.navigate(NotificationsRoute) }, onChangePassword = { nav.navigate(ChangePasswordRoute) },
                        onDeleteAccount = { nav.navigate(DeleteAccountRoute) }, onContact = { nav.navigate(ContactRoute) }, onAbout = { nav.navigate(AboutRoute) },
                    )
                }

                // ---- admin (admins and developers) and teacher tabs: registered only for those roles, so no route or
                // link can open a staff screen in a student's app (the server checks roles as well)
                if (shell.adminPanel) {
                    val openAdmin: (String) -> Unit = { href -> routeForAdminHref(href)?.let { nav.navigate(it) } ?: notAvailable(context) }
                    composable<AdminDashboardTab>(enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) {
                        AdminDashboardScreen(session?.user?.name, session?.user?.role ?: Roles.ADMIN, openAdmin)
                    }
                }
                if (shell == Shell.Teacher) {
                    composable<TeachTab>(enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None }) { TeachHomeScreen(nav) }
                }
                if (shell != Shell.Student) manageGraph(nav)

                // ---- courses
                composable<CourseRoute> { entry ->
                    val r = entry.toRoute<CourseRoute>()
                    CourseDetailScreen(
                        courseId = r.id,
                        onBack = nav::popBackStack,
                        onSignIn = { nav.navigate(SignInRoute) },
                        onPay = { type, title -> nav.navigate(PayRoute(r.id, type, title)) },
                        onOpenLearn = { title -> nav.navigate(LearnRoute(r.id, title)) },
                        onOpenTeacher = { nav.navigate(TeacherRoute(it)) },
                        onCompleteProfile = { nav.navigate(EditProfileRoute) },
                    )
                }
                composable<LearnRoute> { entry ->
                    val r = entry.toRoute<LearnRoute>()
                    LearnScreen(
                        r.courseId, r.title, nav::popBackStack,
                        onPayFee = { nav.navigate(PayRoute(r.courseId, "course", r.title)) },
                        onCourseInfo = { nav.navigate(CourseRoute(r.courseId)) },
                    )
                }
                composable<PayRoute> { entry ->
                    val r = entry.toRoute<PayRoute>()
                    PayScreen(r.courseId, r.type, r.title, nav::popBackStack, onDone = { nav.popBackStack() })
                }
                composable<TeachersRoute> { TeachersScreen(nav::popBackStack) { nav.navigate(TeacherRoute(it)) } }
                composable<TeacherRoute> { TeacherScreen(it.toRoute<TeacherRoute>().id, nav::popBackStack) { id -> nav.navigate(CourseRoute(id)) } }

                // ---- explore
                composable<BlogRoute> { BlogListScreen(nav::popBackStack) { nav.navigate(BlogPostRoute(it)) } }
                composable<BlogPostRoute> { BlogPostScreen(it.toRoute<BlogPostRoute>().id, nav::popBackStack) { nav.navigate(SignInRoute) } }
                composable<FatwaRoute> { FatwaListScreen(nav::popBackStack, { nav.navigate(FatwaDetailRoute(it)) }, { nav.navigate(AskFatwaRoute) }, config?.lookups?.fatwaCategories.orEmpty()) }
                composable<FatwaDetailRoute> { FatwaDetailScreen(it.toRoute<FatwaDetailRoute>().id, nav::popBackStack) }
                composable<AskFatwaRoute> {
                    AskFatwaScreen(
                        nav::popBackStack, session?.user?.name, session?.user?.email,
                        config?.lookups?.fatwaCategories.orEmpty(),
                        config?.brand?.websiteUrl ?: AppConfig().brand.websiteUrl,
                    )
                }
                composable<LibraryRoute> { LibraryListScreen(nav::popBackStack) { nav.navigate(LibraryItemRoute(it)) } }
                composable<LibraryItemRoute> { LibraryItemScreen(it.toRoute<LibraryItemRoute>().id, nav::popBackStack) }
                composable<AnnouncementsRoute> { AnnouncementsScreen(nav::popBackStack) { nav.navigate(AnnouncementRoute(it)) } }
                composable<AnnouncementRoute> { AnnouncementScreen(it.toRoute<AnnouncementRoute>().id, nav::popBackStack) }

                // ---- bookstore
                composable<BookstoreRoute> { BookstoreScreen(nav::popBackStack, { nav.navigate(BookRoute(it)) }, { signedIn { nav.navigate(CartRoute) } }) }
                composable<BookRoute> { BookScreen(it.toRoute<BookRoute>().id, nav::popBackStack, { signedIn { nav.navigate(CartRoute) } }, { nav.navigate(SignInRoute) }) }
                composable<CartRoute> { CartScreen(nav::popBackStack, { nav.navigate(CheckoutRoute) }, { nav.popBackStack() }) }
                composable<CheckoutRoute> { CheckoutScreen(nav::popBackStack) { nav.popBackStack(BookstoreRoute, inclusive = false); nav.navigate(OrdersRoute) } }
                composable<OrdersRoute> { OrdersScreen(nav::popBackStack) { nav.navigate(OrderRoute(it)) } }
                composable<OrderRoute> { OrderScreen(it.toRoute<OrderRoute>().id, nav::popBackStack) }

                // ---- account
                composable<SignInRoute> {
                    SignInScreen(
                        nav::popBackStack, onDone = { nav.popBackStack() }, onRegister = { nav.navigate(RegisterRoute) { popUpTo(SignInRoute) { inclusive = true } } }, onForgot = { nav.navigate(ForgotPasswordRoute) },
                    )
                }
                composable<RegisterRoute> {
                    RegisterScreen(nav::popBackStack, onDone = { nav.popBackStack() }, privacyUrl = config?.policies?.privacyPolicyUrl ?: "/privacy-policy", termsUrl = config?.policies?.termsUrl ?: "/terms-and-conditions")
                }
                composable<ForgotPasswordRoute> { ForgotPasswordScreen(nav::popBackStack) }
                composable<NotificationsRoute> { NotificationsScreen(nav::popBackStack) { href -> openHref(href) } }
                composable<EditProfileRoute> { EditProfileScreen(nav::popBackStack) { nav.switchTab(CoursesTab) } }
                composable<PaymentsRoute> { PaymentsScreen(nav::popBackStack) }
                composable<WaiversRoute> { WaiversScreen(nav::popBackStack) }
                composable<ReviewsRoute> { ReviewsScreen(nav::popBackStack) }
                composable<ChangePasswordRoute> { ChangePasswordScreen(nav::popBackStack) }
                composable<DeleteAccountRoute> { DeleteAccountScreen(nav::popBackStack) { nav.switchTab(HomeTab) } }
                composable<ContactRoute> { ContactScreen(nav::popBackStack) }
                composable<AboutRoute> { AboutScreen(nav::popBackStack) { nav.navigate(TeachersRoute) } }
            }
        }
    }
}

private fun notAvailable(context: android.content.Context) =
    android.widget.Toast.makeText(context, "This section opens on the website for now.", android.widget.Toast.LENGTH_SHORT).show()
