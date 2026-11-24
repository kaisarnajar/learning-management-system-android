package com.darsequran.academy.ui.teach

import kotlinx.serialization.Serializable

/*
 * Teacher portal routes used across sections. The Teach tab (TeachHome.kt) links to all of them; the screens
 * live with their section: the teacher's fatwa questions and blog posts with the content screens
 * (ui/manage/content), the teacher's courses in this package.
 */

/** Fatwa questions assigned to the signed-in teacher (website: /teacher/fatwa). */
@Serializable data object TeacherFatwaRoute

/** The signed-in teacher's blog posts (website: /teacher/blogs). */
@Serializable data object TeacherBlogsRoute
