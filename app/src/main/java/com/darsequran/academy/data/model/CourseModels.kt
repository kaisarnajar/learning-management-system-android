package com.darsequran.academy.data.model

import com.google.gson.annotations.SerializedName

// Attendance DTOs
data class AttendanceRecordDto(
    @SerializedName("id") val id: String,
    @SerializedName("date") val date: String?,
    @SerializedName("isPresent") val isPresent: Boolean
)

data class AttendanceSummaryDto(
    @SerializedName("enrollmentId") val enrollmentId: String,
    @SerializedName("courseId") val courseId: String,
    @SerializedName("rollNumber") val rollNumber: String? = null,
    @SerializedName("totalClasses") val totalClasses: Int = 0,
    @SerializedName("presentClasses") val presentClasses: Int = 0,
    @SerializedName("absentClasses") val absentClasses: Int = 0,
    @SerializedName("percentage") val percentage: Int = 0,
    @SerializedName("records") val records: List<AttendanceRecordDto>? = emptyList()
)

data class StudentAttendanceResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("attendance") val attendance: List<AttendanceSummaryDto>? = emptyList()
)

// Grades DTOs
data class GradeItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("date") val date: String? = null,
    @SerializedName("maxMarks") val maxMarks: Int = 100,
    @SerializedName("marksObtained") val marksObtained: Int = 0,
    @SerializedName("percentage") val percentage: Int = 0
)

data class GradeSummaryDto(
    @SerializedName("enrollmentId") val enrollmentId: String,
    @SerializedName("courseId") val courseId: String,
    @SerializedName("rollNumber") val rollNumber: String? = null,
    @SerializedName("totalMaxMarks") val totalMaxMarks: Int = 0,
    @SerializedName("totalMarksObtained") val totalMarksObtained: Int = 0,
    @SerializedName("overallPercentage") val overallPercentage: Int = 0,
    @SerializedName("grades") val grades: List<GradeItemDto>? = emptyList()
)

data class StudentGradesResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("error") val error: String? = null,
    @SerializedName("grades") val grades: List<GradeSummaryDto>? = emptyList()
)
