package com.darsequran.academy.ui.common

import com.darsequran.academy.ui.theme.SectionTitleStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import android.view.View
import android.text.style.URLSpan
import android.text.style.ImageSpan
import android.text.style.ClickableSpan
import android.text.SpannableStringBuilder
import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil3.compose.AsyncImage
import com.darsequran.academy.R
import com.darsequran.academy.data.resolveUrl
import com.darsequran.academy.ui.theme.ArabicStyle
import com.darsequran.academy.ui.theme.Brand
import com.darsequran.academy.ui.theme.OverlineStyle

// ------------------------------------------------------------- bars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
        },
        actions = { actions() },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabTopBar(title: String, actions: @Composable () -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        actions = { actions() },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

// ------------------------------------------------------- structure

/** Section heading with a gold rule and an optional "See all". */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            splitTitle(title, Brand.gold), Modifier.weight(1f),
            style = SectionTitleStyle, color = MaterialTheme.colorScheme.onBackground,
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(16.dp))
            }
        }
    }
}

/**
 * The website's SplitSectionTitle: uppercase, the last word in gold ("FEATURED **COURSES**"), the rest in the text
 * colour.
 */
fun splitTitle(title: String, accent: Color): AnnotatedString {
    val words = title.trim().uppercase()
    val cut = words.lastIndexOf(' ')
    return buildAnnotatedString {
        if (cut > 0) append(words.substring(0, cut + 1))
        withStyle(SpanStyle(color = accent)) { append(words.substring(cut + 1)) }
    }
}

@Composable
fun GoldRule(width: Dp = 36.dp) {
    Box(Modifier.width(width).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Brand.goldSoft))
}

@Composable
fun Overline(text: String, color: Color = Brand.gold, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = OverlineStyle, color = color, modifier = modifier)
}

/** Card: white surface, hairline outline and a soft shadow, like the website's card-elevated. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier
            .shadow(3.dp, shape, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(shape)
            .background(color)
            .border(1.dp, Brand.outlineSoft, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

private val CardShadow = Color.Black.copy(alpha = 0.12f)

/** The deep-green hero with a faint geometric pattern, mirroring the website's page heroes. */
@Composable
fun HeroBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val hero = Brand.hero
    val deep = Brand.heroDeep
    val gold = Brand.goldSoft
    Box(
        modifier.background(Brush.linearGradient(listOf(hero, deep), start = Offset.Zero, end = Offset.Infinite)),
    ) {
        Canvas(Modifier.matchParentSize().alpha(0.07f)) {
            val step = 44.dp.toPx()
            var y = 0f
            var row = 0
            while (y < size.height + step) {
                var x = if (row % 2 == 0) 0f else step / 2
                while (x < size.width + step) {
                    drawCircle(gold, radius = step / 2.6f, center = Offset(x, y), style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
                    x += step
                }
                y += step * 0.87f
                row++
            }
        }
        content()
    }
}

// ---------------------------------------------------------- status

enum class Tone { Success, Warning, Danger, Info, Neutral }

@Composable
fun StatusChip(label: String, tone: Tone, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tone) {
        Tone.Success -> Brand.successContainer to Brand.success
        Tone.Warning -> Brand.warningContainer to Brand.warning
        Tone.Danger -> Brand.dangerContainer to Brand.danger
        Tone.Info -> Brand.infoContainer to Brand.info
        Tone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        label,
        modifier = modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = fg,
    )
}

/** Enrollment / payment / order status → label and tone. */
fun statusLabel(status: String, kind: StatusKind = StatusKind.Enrollment): Pair<String, Tone> = when (status.lowercase()) {
    // Enrolments, as the website's enrollments.ts words and colours them
    "active" -> "Active" to Tone.Info
    "completed" -> "Completed" to Tone.Success
    "pending_approval" -> "Awaiting approval" to Tone.Warning
    "awaiting_enrollment_fee" -> "Awaiting enrollment fee" to Tone.Warning
    else -> recordStatusLabel(status, kind)
}

/** Payments, book orders, waivers and reviews. */
private fun recordStatusLabel(status: String, kind: StatusKind): Pair<String, Tone> = when (status.lowercase()) {
    "pending_verification" -> "Pending Verification" to Tone.Warning
    "pending" -> when (kind) {
        StatusKind.Waiver -> "Pending Review"
        StatusKind.Review -> "Pending approval"
        else -> "Pending"
    } to Tone.Warning
    "approved" -> "Approved" to Tone.Success
    "rejected" -> "Rejected" to Tone.Danger
    // A declined fee payment reads "Rejected" on the website; a declined book order "Declined".
    "declined" -> (if (kind == StatusKind.Order) "Declined" else "Rejected") to Tone.Danger
    "shipped" -> "Shipped" to Tone.Info
    "refunded" -> "Refunded" to Tone.Neutral
    else -> titleCase(status) to Tone.Neutral
}

/** What a status belongs to, where the website words the same status differently. */
enum class StatusKind { Enrollment, Payment, Order, Waiver, Review }

// ----------------------------------------------------------- states

@Composable
fun LoadingBox(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 3.dp) }
}

@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    message: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (message != null) {
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        if (action != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction, colors = ctaButtonColors()) { Text(action) }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) =
    MessageState(Icons.Rounded.CloudOff, "Couldn't load this", message, "Try again", onRetry, modifier)

@Composable
fun EmptyState(title: String, message: String? = null, action: String? = null, onAction: (() -> Unit)? = null) =
    MessageState(Icons.Rounded.Inbox, title, message, action, onAction)

/** Renders a [Load] with the standard loading and error states. */
@Composable
fun <T> LoadContent(state: Load<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    when (state) {
        Load.Loading -> SkeletonList()
        is Load.Failed -> ErrorState(state.message, onRetry)
        is Load.Ready -> content(state.value)
    }
}

@Composable
fun SkeletonList(rows: Int = 5) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha = transition.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(rows) {
            Box(
                Modifier.fillMaxWidth().height(88.dp).alpha(alpha.value).clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
    }
}

// --------------------------------------------------------- content

/** Remote image with a branded placeholder when there is no picture. */
@Composable
fun RemoteImage(path: String?, contentDescription: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val url = resolveUrl(path)
    if (url == null) {
        Box(modifier.background(Brush.linearGradient(listOf(Brand.hero, Brand.heroDeep))), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.logo), null, Modifier.size(44.dp).alpha(0.5f))
        }
    } else {
        AsyncImage(model = url, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
    }
}

/** Initials avatar or photo. */
@Composable
fun Avatar(name: String?, image: String?, size: Dp = 44.dp) {
    val url = resolveUrl(image)
    if (url != null && !url.contains("/assets/")) {
        AsyncImage(url, name, Modifier.size(size).clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        val initials = (name ?: "?").trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
        Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(initials.ifBlank { "?" }, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

/** Sanitised server HTML (blog bodies, fatwa answers) as native text with tappable links. */
@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier, serif: Boolean = false) {
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    val link = MaterialTheme.colorScheme.primary.toArgb()
    val context = LocalContext.current
    val openLink = LocalOpenLink.current ?: { href: String -> openWeb(context, href) }
    val currentOpen by rememberUpdatedState(openLink)
    // Parsed once per text, not on every recomposition (a like or a new comment redrew the whole article).
    val text = remember(html) { renderHtml(html) { href -> currentOpen(href) } }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = LinkMovementMethod.getInstance()
                textSize = 16f
                setLineSpacing(0f, 1.35f)
                typeface = if (serif) Typeface.SERIF else Typeface.DEFAULT
            }
        },
        update = {
            it.setTextColor(color)
            it.setLinkTextColor(link)
            if (it.text !== text) it.text = text
        },
    )
}

/**
 * Server HTML as styled text. Links call [onLink] with their href, so the app decides where they go (a relative
 * "/blog/x" did nothing as a plain URLSpan, and links to the academy's own pages left the app). Inline images,
 * which a TextView can't show, are left out rather than drawn as an empty box.
 */
fun renderHtml(html: String, onLink: (String) -> Unit): CharSequence {
    val text = SpannableStringBuilder(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT))
    text.getSpans(0, text.length, ImageSpan::class.java).sortedByDescending { text.getSpanStart(it) }.forEach { span ->
        val start = text.getSpanStart(span)
        val end = text.getSpanEnd(span)
        text.removeSpan(span)
        text.delete(start, end)
    }
    text.getSpans(0, text.length, URLSpan::class.java).forEach { span ->
        val start = text.getSpanStart(span)
        val end = text.getSpanEnd(span)
        val flags = text.getSpanFlags(span)
        val href = span.url
        text.removeSpan(span)
        text.setSpan(object : ClickableSpan() { override fun onClick(widget: View) = onLink(href) }, start, end, flags)
    }
    return text.trimEnd()
}

/** Plain text from HTML, for previews in cards. */
fun plainText(html: String?): String =
    HtmlCompat.fromHtml(html ?: "", HtmlCompat.FROM_HTML_MODE_COMPACT).toString().replace('\uFFFC', ' ').replace(Regex("\\s+"), " ").trim()

/** [plainText], worked out once per text rather than on every recomposition of a card. */
@Composable
fun rememberPlainText(html: String?): String = remember(html) { plainText(html) }

/** Arabic text set in the website's Indo-Pak Nastaleeq, right to left. */
@Composable
fun ArabicText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface, size: Int = 26) {
    Text(
        text,
        modifier = modifier,
        style = ArabicStyle.copy(fontSize = size.sp, lineHeight = (size * 2).sp, textAlign = TextAlign.Center),
        color = color,
    )
}

/** Price with the MRP struck through when it is higher. */
@Composable
fun PriceLine(pricePaise: Int, mrpPaise: Int? = null) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(rupees(pricePaise), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        if (mrpPaise != null && mrpPaise > pricePaise) {
            Spacer(Modifier.width(6.dp))
            Text(
                rupees(mrpPaise),
                style = MaterialTheme.typography.bodySmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ctaButtonColors(),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Brand.onCta)
        else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** The website's primary button: gold with dark text. */
@Composable
fun ctaButtonColors() = ButtonDefaults.buttonColors(containerColor = Brand.cta, contentColor = Brand.onCta)

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Inline banner: info, warning or error message inside a screen. */
@Composable
fun Banner(text: String, tone: Tone = Tone.Info, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val (bg, fg) = when (tone) {
        Tone.Success -> Brand.successContainer to Brand.success
        Tone.Warning -> Brand.warningContainer to Brand.warning
        Tone.Danger -> Brand.dangerContainer to Brand.danger
        else -> Brand.infoContainer to Brand.info
    }
    Surface(color = bg, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)) }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = fg)
        }
    }
}
