package com.darsequran.academy.data.staff.courses

import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.data.staff.ActionResponse
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/** Which website portal a course screen stands for. Kept as a string in routes. */
enum class Portal(val path: String) {
    Admin("admin"), Teacher("teacher");

    companion object {
        /** An unknown value gets the teacher portal, the one with fewer rights, never the admin one. */
        fun of(path: String): Portal = entries.firstOrNull { it.path == path } ?: Teacher
    }
}

// ------------------------------------------------------------------ admin course list and form

@Serializable
data class AdminCourseRow(
    val id: String,
    val title: String = "",
    val category: String = "",
    val level: String = "",
    val teacherName: String? = null,
    val duration: String? = null,
    val registrationFeePaise: Int = 0,
    val feePaise: Int = 0,
    val feeFrequency: String = "MONTHLY",
    val feeFrequencyLabel: String = "",
    val studentCount: Int = 0,
    val status: String = "",
    val statusLabel: String = "",
    val featured: Boolean = false,
    val deleteWarning: String? = null,
)

@Serializable
data class CourseHeader(
    val id: String,
    val title: String = "",
    val status: String = "",
    val statusLabel: String = "",
    val category: String = "",
    val level: String = "",
    val startDate: String? = null,
    val duration: String? = null,
    val teacherName: String? = null,
)

@Serializable
data class CourseFormValues(
    val title: String = "",
    val description: String = "",
    val startDate: String = "",
    val duration: String = "",
    val category: String = "",
    val teacherId: String = "",
    val level: String = "Beginner",
    val enrollmentFeeInr: Int = 0,
    val monthlyFeeInr: Int = 0,
    val feeFrequency: String = "MONTHLY",
    val status: String = "PUBLISHED",
    val featuredOnHomepage: Boolean = false,
)

@Serializable data class TeacherOption(val id: String, val name: String = "", val specialization: String = "")
@Serializable data class LevelOption(val value: String, val label: String = value, val defaultFeeInr: Int = 0)
@Serializable data class StatusOption(val value: String, val label: String = value, val description: String = "")
@Serializable data class ValueLabel(val value: String, val label: String = value)

@Serializable
data class CourseFormOptions(
    val teachers: List<TeacherOption> = emptyList(),
    val categories: List<String> = emptyList(),
    val levels: List<LevelOption> = emptyList(),
    val statuses: List<StatusOption> = emptyList(),
    val feeFrequencies: List<ValueLabel> = emptyList(),
    val featuredCount: Int = 0,
    val featuredMax: Int = 6,
)

@Serializable
data class CourseFormResponse(
    val course: CourseHeader? = null,
    val values: CourseFormValues = CourseFormValues(),
    val options: CourseFormOptions = CourseFormOptions(),
)

@Serializable
data class RosterRow(
    val enrollmentId: String,
    val userId: String? = null,
    val name: String? = null,
    val email: String = "",
    val rollNumber: Int? = null,
    val rollNumberLabel: String = "—",
    val status: String? = null,
    val statusLabel: String? = null,
)

interface AdminCoursesApi {
    @GET("admin/courses") suspend fun list(@Query("page") page: Int, @Query("q") q: String?): Paged<AdminCourseRow>
    @GET("admin/courses/new") suspend fun newForm(): CourseFormResponse
    @GET("admin/courses/{id}") suspend fun get(@Path("id") id: String): CourseFormResponse
    @Multipart @POST("admin/courses") suspend fun create(@Part parts: List<MultipartBody.Part>): ActionResponse
    @Multipart @PATCH("admin/courses/{id}") suspend fun update(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse
    @DELETE("admin/courses/{id}") suspend fun delete(@Path("id") id: String): ActionResponse
    @GET("admin/courses/{id}/students") suspend fun roster(@Path("id") id: String, @Query("page") page: Int, @Query("q") q: String?): Paged<RosterRow>

    @Streaming @GET("admin/courses/{id}/{kind}/student/{enrollmentId}/card")
    suspend fun card(@Path("id") id: String, @Path("kind") kind: String, @Path("enrollmentId") enrollmentId: String): ResponseBody

    @POST("admin/courses/{id}/{kind}/student/{enrollmentId}/card/email")
    suspend fun emailCard(@Path("id") id: String, @Path("kind") kind: String, @Path("enrollmentId") enrollmentId: String): ActionResponse
}

// ------------------------------------------------------------------ shared course tools

@Serializable data class CourseHeaderResponse(val course: CourseHeader)

@Serializable
data class CourseAnnouncement(
    val id: String,
    val category: String = "",
    val categoryLabel: String = "",
    val title: String = "",
    val body: String = "",
    val authorName: String = "",
    val postedByAdmin: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val edited: Boolean = false,
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
    val canManage: Boolean = true,
)

@Serializable data class AnnouncementResponse(val announcement: CourseAnnouncement, val categories: List<ValueLabel> = emptyList())

@Serializable
data class AttendanceDay(
    val id: String,
    val date: String,
    val dateLabel: String = date,
    val present: Int = 0,
    val absent: Int = 0,
    val total: Int = 0,
)

@Serializable data class AttendanceListResponse(val course: CourseHeader? = null, val data: List<AttendanceDay> = emptyList())

@Serializable
data class GradableStudent(
    val enrollmentId: String,
    val rollNumber: Int? = null,
    val rollNumberLabel: String = "—",
    val name: String? = null,
    val email: String = "",
    val isPresent: Boolean? = null,
    val marksObtained: Double? = null,
)

@Serializable data class StudentsResponse(val data: List<GradableStudent> = emptyList())

@Serializable
data class AttendanceDayDetail(
    val date: String,
    val dateLabel: String = date,
    val present: Int = 0,
    val absent: Int = 0,
    val total: Int = 0,
    val records: List<GradableStudent> = emptyList(),
)

@Serializable data class AttendanceMark(val enrollmentId: String, val isPresent: Boolean)
@Serializable data class AttendanceSave(val records: List<AttendanceMark>)

@Serializable data class ReportStudent(val enrollmentId: String, val name: String? = null, val email: String = "", val rollNumberLabel: String = "—")

@Serializable data class AttendanceSummary(val total: Int = 0, val present: Int = 0, val absent: Int = 0, val percentage: Int = 0)
@Serializable data class AttendanceRecord(val id: String, val date: String, val dateLabel: String = date, val isPresent: Boolean)

@Serializable
data class AttendanceReport(
    val student: ReportStudent,
    val courseTitle: String = "",
    val summary: AttendanceSummary = AttendanceSummary(),
    val records: List<AttendanceRecord> = emptyList(),
    val canDownloadCard: Boolean = false,
)

@Serializable
data class GradeCardRow(val id: String, val title: String = "", val date: String = "", val dateLabel: String = date, val maxMarks: Int = 0, val graded: Int = 0)

@Serializable data class GradeListResponse(val course: CourseHeader? = null, val data: List<GradeCardRow> = emptyList())

@Serializable
data class GradeDetail(val id: String, val title: String = "", val date: String = "", val dateLabel: String = date, val maxMarks: Int = 0, val records: List<GradableStudent> = emptyList())

@Serializable data class GradeResponse(val grade: GradeDetail)

@Serializable data class GradeMark(val enrollmentId: String, val marksObtained: Double?)
@Serializable data class GradeSave(val title: String, val date: String, val maxMarks: Int, val records: List<GradeMark>)

@Serializable data class GradeSummary(val exams: Int = 0, val obtained: Double = 0.0, val max: Int = 0, val percentage: Int = 0)
@Serializable data class GradeRecord(val id: String, val title: String = "", val date: String = "", val dateLabel: String = date, val maxMarks: Int = 0, val marksObtained: Double = 0.0)

@Serializable
data class GradeReport(
    val student: ReportStudent,
    val courseTitle: String = "",
    val summary: GradeSummary = GradeSummary(),
    val records: List<GradeRecord> = emptyList(),
    val canDownloadCard: Boolean = false,
)

/** `portal` is [Portal.path]: "admin" or "teacher". Teachers reach only their own courses. */
interface CourseToolsApi {
    @GET("{portal}/courses/{id}") suspend fun course(@Path("portal") portal: String, @Path("id") id: String): CourseHeaderResponse

    // announcements (enrollmentId: a teacher's private messages to one student)
    @GET("{portal}/courses/{id}/announcements")
    suspend fun announcements(@Path("portal") portal: String, @Path("id") id: String, @Query("page") page: Int, @Query("q") q: String?): Paged<CourseAnnouncement>

    @GET("{portal}/courses/{id}/students/{enrollmentId}/announcements")
    suspend fun messages(@Path("portal") portal: String, @Path("id") id: String, @Path("enrollmentId") enrollmentId: String, @Query("page") page: Int): Paged<CourseAnnouncement>

    @GET("{portal}/courses/{id}/{scope}announcements/{announcementId}")
    suspend fun announcement(@Path("portal") portal: String, @Path("id") id: String, @Path("scope", encoded = true) scope: String, @Path("announcementId") announcementId: String): AnnouncementResponse

    @Multipart @POST("{portal}/courses/{id}/{scope}announcements")
    suspend fun createAnnouncement(@Path("portal") portal: String, @Path("id") id: String, @Path("scope", encoded = true) scope: String, @Part parts: List<MultipartBody.Part>): ActionResponse

    @Multipart @PATCH("{portal}/courses/{id}/{scope}announcements/{announcementId}")
    suspend fun updateAnnouncement(@Path("portal") portal: String, @Path("id") id: String, @Path("scope", encoded = true) scope: String, @Path("announcementId") announcementId: String, @Part parts: List<MultipartBody.Part>): ActionResponse

    @DELETE("{portal}/courses/{id}/{scope}announcements/{announcementId}")
    suspend fun deleteAnnouncement(@Path("portal") portal: String, @Path("id") id: String, @Path("scope", encoded = true) scope: String, @Path("announcementId") announcementId: String): ActionResponse

    // attendance
    @GET("{portal}/courses/{id}/attendance") suspend fun attendance(@Path("portal") portal: String, @Path("id") id: String): AttendanceListResponse
    @GET("{portal}/courses/{id}/attendance/students") suspend fun attendanceStudents(@Path("portal") portal: String, @Path("id") id: String): StudentsResponse
    @GET("{portal}/courses/{id}/attendance/{date}") suspend fun attendanceDay(@Path("portal") portal: String, @Path("id") id: String, @Path("date") date: String): AttendanceDayDetail
    @PUT("{portal}/courses/{id}/attendance/{date}") suspend fun saveAttendance(@Path("portal") portal: String, @Path("id") id: String, @Path("date") date: String, @Body body: AttendanceSave): ActionResponse
    @DELETE("{portal}/courses/{id}/attendance/{date}") suspend fun deleteAttendance(@Path("portal") portal: String, @Path("id") id: String, @Path("date") date: String): ActionResponse
    @GET("{portal}/courses/{id}/attendance/student/{enrollmentId}") suspend fun attendanceReport(@Path("portal") portal: String, @Path("id") id: String, @Path("enrollmentId") enrollmentId: String): AttendanceReport

    // grade cards
    @GET("{portal}/courses/{id}/grades") suspend fun grades(@Path("portal") portal: String, @Path("id") id: String): GradeListResponse
    @GET("{portal}/courses/{id}/grades/students") suspend fun gradeStudents(@Path("portal") portal: String, @Path("id") id: String): StudentsResponse
    @GET("{portal}/courses/{id}/grades/{gradeId}") suspend fun grade(@Path("portal") portal: String, @Path("id") id: String, @Path("gradeId") gradeId: String): GradeResponse
    @POST("{portal}/courses/{id}/grades") suspend fun createGrade(@Path("portal") portal: String, @Path("id") id: String, @Body body: GradeSave): ActionResponse
    @PUT("{portal}/courses/{id}/grades/{gradeId}") suspend fun updateGrade(@Path("portal") portal: String, @Path("id") id: String, @Path("gradeId") gradeId: String, @Body body: GradeSave): ActionResponse
    @DELETE("{portal}/courses/{id}/grades/{gradeId}") suspend fun deleteGrade(@Path("portal") portal: String, @Path("id") id: String, @Path("gradeId") gradeId: String): ActionResponse
    @GET("{portal}/courses/{id}/grades/student/{enrollmentId}") suspend fun gradeReport(@Path("portal") portal: String, @Path("id") id: String, @Path("enrollmentId") enrollmentId: String): GradeReport
}

/** The URL piece between the course and "announcements": "" for course-wide posts, "students/{id}/" for a student's messages. */
fun announcementScope(enrollmentId: String?): String = if (enrollmentId == null) "" else "students/$enrollmentId/"
