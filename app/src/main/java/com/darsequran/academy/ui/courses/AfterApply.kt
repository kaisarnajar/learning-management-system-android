package com.darsequran.academy.ui.courses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Goes where the answer to an application sends the student (see [CourseDetailViewModel.next]). */
@Composable
internal fun AfterApplyEffect(
    vm: CourseDetailViewModel,
    title: String?,
    onPay: (type: String, title: String) -> Unit,
    onCompleteProfile: () -> Unit,
) {
    val next by vm.next.collectAsStateWithLifecycle()
    LaunchedEffect(next, title) {
        if (title == null) return@LaunchedEffect
        when (next) {
            CourseDetailViewModel.AfterApply.PAY -> onPay("enrollment", title)
            CourseDetailViewModel.AfterApply.COMPLETE_PROFILE -> onCompleteProfile()
            null -> return@LaunchedEffect
        }
        vm.consumeNext()
    }
}
