package com.darsequran.academy.data.remote

/** The account roles the website gives users ([SessionUser.role]). */
object Roles {
    const val USER = "USER"
    const val TEACHER = "TEACHER"
    const val ADMIN = "ADMIN"
    const val DEVELOPER = "DEVELOPER"

    /** Admins and developers use the admin area. */
    fun isAdmin(role: String?): Boolean = role == ADMIN || role == DEVELOPER
}

/** The [Enrollment.status] values the app acts on. */
object EnrollmentStatus {
    const val ACTIVE = "active"
    const val COMPLETED = "completed"
    const val AWAITING_ENROLLMENT_FEE = "awaiting_enrollment_fee"
    const val PENDING_APPROVAL = "pending_approval"

    /** Studying or finished: the course's own screens (updates, attendance, marks, fees) are open to the student. */
    fun opensLearn(status: String): Boolean = status == ACTIVE || status == COMPLETED
}
