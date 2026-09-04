package com.darsequran.academy.ui.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.darsequran.academy.ui.announcements.AnnouncementsScreen
import com.darsequran.academy.ui.blog.BlogScreen
import com.darsequran.academy.ui.blog.BlogViewModel
import com.darsequran.academy.ui.courses.CourseDetailBottomSheet
import com.darsequran.academy.ui.courses.CoursesCatalogScreen
import com.darsequran.academy.ui.courses.CoursesCatalogViewModel
import com.darsequran.academy.ui.fatwa.FatwaScreen
import com.darsequran.academy.ui.fatwa.FatwaViewModel
import com.darsequran.academy.ui.home.HomeViewModel
import com.darsequran.academy.ui.teachers.TeachersScreen
import com.darsequran.academy.ui.teachers.TeachersViewModel
import com.darsequran.academy.ui.theme.EmeraldDark
import com.darsequran.academy.ui.theme.GoldAccent

@Composable
fun ExploreMainScreen(
    catalogViewModel: CoursesCatalogViewModel,
    teachersViewModel: TeachersViewModel,
    blogViewModel: BlogViewModel,
    fatwaViewModel: FatwaViewModel,
    homeViewModel: HomeViewModel
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val coursesState by catalogViewModel.uiState.collectAsState()
    val tabs = listOf("Courses", "Teachers", "Announcements", "Blog", "Fatwa")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = EmeraldDark,
                contentColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = GoldAccent
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) GoldAccent else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> CoursesCatalogScreen(
                    viewModel = catalogViewModel,
                    onBackPress = {},
                    onTeacherClick = { teacherName ->
                        catalogViewModel.selectTeacherDetailByName(teacherName)
                    }
                )
                1 -> TeachersScreen(
                    viewModel = teachersViewModel,
                    courses = coursesState.courses,
                    onCourseClick = { course ->
                        catalogViewModel.selectCourseDetail(course)
                    },
                    onBackPress = {}
                )
                2 -> AnnouncementsScreen(viewModel = homeViewModel)
                3 -> BlogScreen(viewModel = blogViewModel, onBackPress = {})
                4 -> FatwaScreen(viewModel = fatwaViewModel, onBackPress = {})
            }
        }
    }

    // Global Course Detail Bottom Sheet Modal (renders on current active tab without changing tabs)
    coursesState.selectedCourseDetail?.let { course ->
        CourseDetailBottomSheet(
            course = course,
            isEnrolling = coursesState.isEnrolling,
            onDismissRequest = { catalogViewModel.selectCourseDetail(null) },
            onRequestEnrollment = { courseId -> catalogViewModel.requestEnrollment(courseId) },
            onTeacherClick = { teacherName, specialization ->
                catalogViewModel.selectCourseDetail(null)
                catalogViewModel.selectTeacherDetailByName(teacherName, specialization)
            }
        )
    }
}
