package com.darsequran.academy.ui.manage

import kotlinx.serialization.Serializable

// The Admin (or Developer) tab: the website's admin dashboard, with every sidebar group and its sections.
@Serializable data object AdminDashboardTab

// Teacher tab (teachers keep the student tabs and get this one too).
@Serializable data object TeachTab

// Developer
@Serializable data object EnvironmentRoute
