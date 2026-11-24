package com.darsequran.academy.ui.manage.content

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.darsequran.academy.ui.teach.TeacherBlogsRoute
import com.darsequran.academy.ui.teach.TeacherFatwaRoute
import kotlinx.serialization.Serializable

// ------------------------------------------------------------------ routes

@Serializable data object AnnouncementsAdminRoute
/** [id] null: a new announcement. */
@Serializable data class SiteAnnouncementFormRoute(val id: String? = null)

@Serializable data object BlogsAdminRoute
@Serializable data class BlogFormRoute(val id: String? = null)
@Serializable data object BlogApprovalsRoute
@Serializable data class BlogApprovalRoute(val id: String)
@Serializable data class TeacherBlogFormRoute(val id: String? = null)

@Serializable data object FatwaAdminRoute
@Serializable data class FatwaAdminDetailRoute(val id: String)
@Serializable data class TeacherFatwaDetailRoute(val id: String)

@Serializable data object LibraryAdminRoute
@Serializable data class LibraryAdminDetailRoute(val id: String)
@Serializable data class LibraryFormRoute(val id: String? = null)

@Serializable data object InspirationAdminRoute
@Serializable data class InspirationDetailRoute(val id: String)
@Serializable data class InspirationFormRoute(val id: String? = null)

// ------------------------------------------------------------------- graph

fun NavGraphBuilder.contentGraph(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }

    // Site announcements
    composable<AnnouncementsAdminRoute> {
        AnnouncementsListScreen(back, onOpen = { nav.navigate(SiteAnnouncementFormRoute(it)) }, onNew = { nav.navigate(SiteAnnouncementFormRoute()) })
    }
    composable<SiteAnnouncementFormRoute> { SiteAnnouncementFormScreen(it.toRoute<SiteAnnouncementFormRoute>().id, back) }

    // Blogs and approvals (admin)
    composable<BlogsAdminRoute> { BlogsListScreen(back, onOpen = { nav.navigate(BlogFormRoute(it)) }, onNew = { nav.navigate(BlogFormRoute()) }) }
    composable<BlogFormRoute> { BlogEditorScreen(BlogEditorMode.Admin, it.toRoute<BlogFormRoute>().id, back) }
    composable<BlogApprovalsRoute> {
        BlogApprovalsListScreen(back, onOpen = { nav.navigate(BlogApprovalRoute(it)) }, onEdit = { nav.navigate(BlogFormRoute(it)) })
    }
    composable<BlogApprovalRoute> {
        val id = it.toRoute<BlogApprovalRoute>().id
        BlogApprovalDetailScreen(id, back, onEdit = { nav.navigate(BlogFormRoute(id)) })
    }

    // Fatwa (admin)
    composable<FatwaAdminRoute> { FatwaListScreen(FatwaAudience.Admin, back) { nav.navigate(FatwaAdminDetailRoute(it)) } }
    composable<FatwaAdminDetailRoute> { FatwaAnswerScreen(FatwaAudience.Admin, it.toRoute<FatwaAdminDetailRoute>().id, back) }

    // Digital library
    composable<LibraryAdminRoute> {
        LibraryListScreen(
            back,
            onOpen = { nav.navigate(LibraryAdminDetailRoute(it)) },
            onEdit = { nav.navigate(LibraryFormRoute(it)) },
            onNew = { nav.navigate(LibraryFormRoute()) },
        )
    }
    composable<LibraryAdminDetailRoute> {
        val id = it.toRoute<LibraryAdminDetailRoute>().id
        LibraryDetailScreen(id, back, onEdit = { nav.navigate(LibraryFormRoute(id)) })
    }
    composable<LibraryFormRoute> { LibraryFormScreen(it.toRoute<LibraryFormRoute>().id, back) }

    // Verse & hadith of the day
    composable<InspirationAdminRoute> {
        InspirationListScreen(
            back,
            onOpen = { nav.navigate(InspirationDetailRoute(it)) },
            onEdit = { nav.navigate(InspirationFormRoute(it)) },
            onNew = { nav.navigate(InspirationFormRoute()) },
        )
    }
    composable<InspirationDetailRoute> {
        val id = it.toRoute<InspirationDetailRoute>().id
        InspirationDetailScreen(id, back, onEdit = { nav.navigate(InspirationFormRoute(id)) })
    }
    composable<InspirationFormRoute> { InspirationFormScreen(it.toRoute<InspirationFormRoute>().id, back) }

    // Teacher portal: the teacher's fatwa answers and blog posts, with the admin screens' editors
    composable<TeacherFatwaRoute> { FatwaListScreen(FatwaAudience.Teacher, back) { nav.navigate(TeacherFatwaDetailRoute(it)) } }
    composable<TeacherFatwaDetailRoute> { FatwaAnswerScreen(FatwaAudience.Teacher, it.toRoute<TeacherFatwaDetailRoute>().id, back) }
    composable<TeacherBlogsRoute> {
        TeacherBlogsListScreen(back, onEdit = { nav.navigate(TeacherBlogFormRoute(it)) }, onNew = { nav.navigate(TeacherBlogFormRoute()) })
    }
    composable<TeacherBlogFormRoute> { BlogEditorScreen(BlogEditorMode.Teacher, it.toRoute<TeacherBlogFormRoute>().id, back) }
}
