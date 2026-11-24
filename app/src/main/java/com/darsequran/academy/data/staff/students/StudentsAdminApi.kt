package com.darsequran.academy.data.staff.students

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.darsequran.academy.data.remote.Paged
import com.darsequran.academy.data.staff.ActionResponse
import com.darsequran.academy.ui.common.statusLabel
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@Serializable
data class StudentRow(
    val id: String,
    val name: String? = null,
    val email: String = "",
    val registrationNumber: String? = null,
    val image: String? = null,
    val gender: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class StudentEnrollment(
    val id: String,
    val courseId: String,
    val courseTitle: String = "",
    val rollNumber: String? = null,
    val status: String = "",
    val statusLabel: String = "",
    val createdAt: String? = null,
)

@Serializable
data class StudentDetail(
    val id: String,
    val name: String? = null,
    val email: String = "",
    val registrationNumber: String? = null,
    val image: String? = null,
    val gender: String? = null,
    val fatherName: String? = null,
    val dateOfBirth: String? = null,
    val dateOfBirthDisplay: String? = null,
    val occupation: String? = null,
    val occupationLabel: String? = null,
    val address: String? = null,
    val whatsapp: String? = null,
    val createdAt: String? = null,
    val profileComplete: Boolean = false,
    val enrollments: List<StudentEnrollment> = emptyList(),
)

@Serializable data class StudentResponse(val student: StudentDetail)

@Serializable
data class StudentPayment(
    val id: String,
    val paidAt: String? = null,
    val amountInrPaise: Int = 0,
    val courseTitle: String? = null,
    val description: String? = null,
)

interface StudentsAdminApi {
    @GET("admin/students")
    suspend fun list(@Query("page") page: Int, @Query("q") q: String?, @Query("sort") sort: String?): Paged<StudentRow>

    @GET("admin/students/{id}") suspend fun get(@Path("id") id: String): StudentResponse

    @GET("admin/students/{id}/payments")
    suspend fun payments(@Path("id") id: String, @Query("page") page: Int = 1, @Query("pageSize") pageSize: Int = 50): Paged<StudentPayment>

    @Multipart @PATCH("admin/students/{id}")
    suspend fun update(@Path("id") id: String, @Part parts: List<MultipartBody.Part>): ActionResponse

    @DELETE("admin/students/{id}") suspend fun delete(@Path("id") id: String): ActionResponse

    @Streaming @GET("admin/students/{id}/id-card") suspend fun idCard(@Path("id") id: String): ResponseBody

    @POST("admin/students/{id}/id-card/email") suspend fun emailIdCard(@Path("id") id: String): ActionResponse

}

// --------------------------------------------------------------- routes
