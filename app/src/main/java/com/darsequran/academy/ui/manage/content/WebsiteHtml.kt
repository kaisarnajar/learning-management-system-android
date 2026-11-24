package com.darsequran.academy.ui.manage.content

/*
 * The rich text editor (RichTextField) writes inline formatting as CSS on <span>s, e.g.
 * `<span style="font-weight: bold; font-style: italic;">`. The website's sanitiser (services/rich-text.ts) keeps
 * only `color` and `text-align` styles, so that bold, italic, underline, strikethrough and headings would be lost
 * when saved. Before sending, [toWebsiteHtml] rewrites those styles as the tags the sanitiser keeps (strong, em,
 * u, s, h2) — the same tags the website's own editor produces — and the editor reads them back on the next edit.
 */

private val token = Regex("<[^>]*>|[^<]+")
private val styleAttr = Regex("""style\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
private val allowedColor = Regex("""^(#[0-9a-fA-F]{3,8}|rgb\(\s*\d{1,3}\s*,\s*\d{1,3}\s*,\s*\d{1,3}\s*\)|[a-z]{3,20})$""")
private const val HEADING_MARK = "<strong data-heading>"

/** Editor HTML → HTML whose formatting survives the website's sanitiser. */
fun toWebsiteHtml(editorHtml: String): String {
    val out = StringBuilder()
    val closers = ArrayDeque<String>()
    for (match in token.findAll(editorHtml)) {
        val t = match.value
        val lower = t.lowercase()
        when {
            lower.startsWith("<span") -> {
                val (open, close) = spanReplacement(t)
                out.append(open)
                closers.addLast(close)
            }
            lower.startsWith("</span") -> out.append(closers.removeLastOrNull() ?: "")
            else -> out.append(t)
        }
    }
    // A paragraph that is all heading becomes an <h2> (keeping its alignment); other heading runs stay bold.
    val html = Regex("""<p([^>]*)>\s*${Regex.escape(HEADING_MARK)}((?:(?!</?(?:p|strong)[\s>]).)*)</strong>\s*</p>""", RegexOption.DOT_MATCHES_ALL)
        .replace(out.toString()) { m -> "<h2${m.groupValues[1]}>${m.groupValues[2]}</h2>" }
    return html.replace(HEADING_MARK, "<strong>")
}

/** The opening and closing markup that replace one `<span style="…">`. */
private fun spanReplacement(tag: String): Pair<String, String> {
    val styles = styleAttr.find(tag)?.groupValues?.get(1).orEmpty()
        .split(';')
        .mapNotNull { decl -> decl.split(':', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim().lowercase() to it[1].trim() } }
        .toMap()
    val wrappers = mutableListOf<String>()
    val heading = styles["font-size"]?.let(::isHeadingSize) == true
    val bold = styles["font-weight"]?.lowercase()?.let { it == "bold" || it == "bolder" || (it.toIntOrNull() ?: 0) >= 600 } == true
    if (heading) wrappers += "strong data-heading" else if (bold) wrappers += "strong"
    if (styles["font-style"]?.lowercase()?.let { it == "italic" || it == "oblique" } == true) wrappers += "em"
    val decoration = styles["text-decoration"]?.lowercase().orEmpty() + " " + styles["text-decoration-line"]?.lowercase().orEmpty()
    if ("underline" in decoration) wrappers += "u"
    if ("line-through" in decoration) wrappers += "s"
    val color = styles["color"]?.takeIf { allowedColor.matches(it) }

    val open = StringBuilder()
    val close = StringBuilder()
    if (color != null) open.append("<span style=\"color: $color\">")
    wrappers.forEach { open.append('<').append(it).append('>') }
    wrappers.asReversed().forEach { close.append("</").append(it.substringBefore(' ')).append('>') }
    if (color != null) close.append("</span>")
    return open.toString() to close.toString()
}

/** The toolbar's heading is 22sp (body text 16): anything from 1.2em / 18px up counts as a heading. */
private fun isHeadingSize(value: String): Boolean {
    val number = Regex("""[\d.]+""").find(value)?.value?.toFloatOrNull() ?: return false
    val unit = value.lowercase().replace(Regex("""[\d.\s]"""), "")
    return when (unit) {
        "em", "rem" -> number >= 1.2f
        "%" -> number >= 120f
        else -> number >= 18f
    }
}

/** True when rich text has no visible words (the editor writes `<p></p>` for an empty document). */
fun isBlankHtml(html: String): Boolean = html.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").isBlank()
