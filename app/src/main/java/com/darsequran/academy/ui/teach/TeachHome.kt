package com.darsequran.academy.ui.teach

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.darsequran.academy.ui.common.EmptyState
import com.darsequran.academy.ui.manage.common.ManageScaffold

/** The teacher's tab: their courses, fatwa questions and blog posts. */
@Composable
fun TeachHomeScreen(nav: NavHostController) {
    ManageScaffold("Teach", onBack = null) { _ -> EmptyState("Your classes", "Coming in this build.") }
}

/** The teacher portal's screens. */
fun NavGraphBuilder.teachGraph(nav: NavHostController) {
}
