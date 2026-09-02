package com.darsequran.academy.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object CoursesCatalog : Screen("courses_catalog")
    object DigitalLibrary : Screen("digital_library")
    object Announcements : Screen("announcements")
    object Blog : Screen("blog")
    object Teachers : Screen("teachers")
    object Fatwa : Screen("fatwa")
    object Bookstore : Screen("bookstore")
}
