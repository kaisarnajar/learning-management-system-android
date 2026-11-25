package com.darsequran.academy.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

/*
 * How pages move. Opening a page slides it in from the right over the current one, which drifts a little to the
 * left; Back is the same movement in reverse: the page slides all the way out to the right and uncovers the one
 * underneath as it drifts back into place. Nothing fades, so the page never seems to vanish, and the same transitions
 * follow the finger during a predictive back swipe. The navigation host draws the page on top that is opening (or,
 * on Back, closing), so the covered page is always the one drifting.
 */

/** Long enough to read as movement, short enough not to slow anyone down (Android's own run 300–450 ms). */
private const val PAGE_MS = 350

/** How far the covered page drifts: a quarter of the width, which gives depth without a gap at the edge. */
private const val DRIFT_DIVISOR = 4

private val pageSpec = tween<IntOffset>(PAGE_MS, easing = FastOutSlowInEasing)

/** A page opening: in from the right edge. */
val pageEnter: EnterTransition = slideInHorizontally(pageSpec) { width -> width }

/** The page an opening one covers: drifts left. */
val pageExit: ExitTransition = slideOutHorizontally(pageSpec) { width -> -width / DRIFT_DIVISOR }

/** On Back, the page uncovered: drifts back from the left. */
val pagePopEnter: EnterTransition = slideInHorizontally(pageSpec) { width -> -width / DRIFT_DIVISOR }

/** On Back, the page closing: out past the right edge. */
val pagePopExit: ExitTransition = slideOutHorizontally(pageSpec) { width -> width }
