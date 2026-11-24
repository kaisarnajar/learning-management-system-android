package com.darsequran.academy.data.staff.teach

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.data.staff.courses.CourseHeader
import com.darsequran.academy.data.staff.courses.RosterRow
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class TeacherCourseRow(
    val id: String,
    val title: String = "",
    val status: String = "",
    val statusLabel: String = "",
    val level: String = "",
    val category: String = "",
    val startDate: String? = null,
    val duration: String? = null,
    val description: String = "",
    val studentCount: Int = 0,
)

@Serializable data class TeacherProfile(val name: String = "", val specialization: String = "", val email: String = "")
@Serializable data class TeacherStats(val totalCourses: Int = 0, val totalStudents: Int = 0)

@Serializable
data class TeacherHome(
    val data: List<TeacherCourseRow> = emptyList(),
    val totalCount: Int = 0,
    val teacher: TeacherProfile = TeacherProfile(),
    val stats: TeacherStats = TeacherStats(),
)

@Serializable
data class TeacherEnrollment(
    val id: String,
    val status: String = "",
    val statusLabel: String = "",
    val rollNumberLabel: String = "—",
    val createdAt: String? = null,
    val completedAt: String? = null,
)

@Serializable
data class TeacherStudentProfile(
    val id: String,
    val name: String = "",
    val email: String = "",
    val fatherName: String? = null,
    val dateOfBirthDisplay: String? = null,
    val occupationLabel: String? = null,
    val address: String? = null,
    val whatsapp: String? = null,
    val createdAt: String? = null,
)

@Serializable data class TeacherStudentResponse(val course: CourseHeader, val enrollment: TeacherEnrollment, val student: TeacherStudentProfile)

interface TeachApi {
    @GET("teacher/courses") suspend fun home(@Query("page") page: Int = 1): TeacherHome
    @GET("teacher/courses") suspend fun courses(@Query("page") page: Int): Paged<TeacherCourseRow>
    @GET("teacher/courses/{id}/students") suspend fun students(@Path("id") id: String, @Query("page") page: Int): Paged<RosterRow>
    @GET("teacher/courses/{id}/students/{enrollmentId}") suspend fun student(@Path("id") id: String, @Path("enrollmentId") enrollmentId: String): TeacherStudentResponse
}

// ------------------------------------------------------------------ routes
