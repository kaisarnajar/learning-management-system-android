package com.darsequran.academy.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.Session
import com.darsequran.academy.data.remote.AskFatwaRequest
import com.darsequran.academy.data.remote.BlogPost
import com.darsequran.academy.data.remote.Comment
import com.darsequran.academy.data.remote.Fatwa
import com.darsequran.academy.data.remote.LibraryItem
import com.darsequran.academy.data.remote.SiteAnnouncement
import com.darsequran.academy.data.remote.Teacher
import com.darsequran.academy.data.remote.TeacherDetailResponse
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.data.repo.AuthRepository
import com.darsequran.academy.ui.common.countOf
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Avatar
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.GoldRule
import com.darsequran.academy.ui.common.HtmlText
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadContent
import com.darsequran.academy.ui.common.MessageState
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PagedListBody
import com.darsequran.academy.ui.common.PagedViewModel
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.RemoteImage
import com.darsequran.academy.ui.common.SecondaryButton
import com.darsequran.academy.ui.common.TabTopBar
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.openWeb
import com.darsequran.academy.ui.common.rememberPlainText
import com.darsequran.academy.ui.common.relativeTime
import com.darsequran.academy.ui.common.toLoad
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.home.BlogRow
import com.darsequran.academy.ui.home.FatwaRow
import com.darsequran.academy.ui.courses.CourseCard
import com.darsequran.academy.ui.theme.Brand
import com.darsequran.academy.ui.common.Dropdown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// =================================================================== hub

@Composable
fun ExploreScreen(
    onBlog: () -> Unit,
    onFatwa: () -> Unit,
    onLibrary: () -> Unit,
    onAnnouncements: () -> Unit,
    onTeachers: () -> Unit,
    onBookstore: () -> Unit,
) {
    Scaffold(topBar = { TabTopBar("Explore") }) { padding ->
        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Text("Learn, read and ask — the academy's knowledge in one place.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
            }
            val tiles = listOf(
                Triple("Fatwa", "Questions answered by scholars", Icons.Rounded.QuestionAnswer) to onFatwa,
                Triple("Blog", "Articles from our teachers", Icons.AutoMirrored.Rounded.MenuBook) to onBlog,
                Triple("Library", "Books to read and download", Icons.Rounded.LocalLibrary) to onLibrary,
                Triple("Bookstore", "Order printed books", Icons.Rounded.Storefront) to onBookstore,
                Triple("Announcements", "News and events", Icons.Rounded.Campaign) to onAnnouncements,
                Triple("Teachers", "Meet our scholars", Icons.Rounded.Groups) to onTeachers,
            )
            items(tiles) { (info, action) -> HubTile(info.first, info.second, info.third, action) }
        }
    }
}

@Composable
private fun HubTile(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth().height(150.dp), onClick = onClick) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.weight(1f))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

// ================================================================== blog

@Composable
fun BlogListScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm: PagedViewModel<BlogPost> = viewModel(key = "blog", factory = vmFactory { c -> PagedViewModel { p, q, _ -> c.repo.blog(p, q) } })
    Scaffold(topBar = { BackTopBar("Blog", onBack) }) { padding ->
        PagedListBody(vm, "Search articles", "No articles yet", { it.id }, Modifier.padding(padding)) { BlogRow(it) { onOpen(it.id) } }
    }
}

data class PostState(val post: Load<BlogPost> = Load.Loading, val comments: List<Comment> = emptyList(), val busy: Boolean = false, val error: String? = null)

class BlogPostViewModel(private val repo: AcademyRepository, private val auth: AuthRepository, private val id: String) : ViewModel() {
    private val _state = MutableStateFlow(PostState())
    val state: StateFlow<PostState> = _state.asStateFlow()
    val session: StateFlow<Session?> get() = auth.session

    private var liking = false

    init {
        load()
        // "Liked by me" and comment rights depend on who is signed in: reload when that changes.
        viewModelScope.launch { auth.session.map { it?.user?.id }.distinctUntilChanged().drop(1).collect { load() } }
    }

    fun load() = viewModelScope.launch {
        val r = repo.blogPost(id)
        _state.update { it.copy(post = r.toLoad(), comments = (r as? ApiResult.Ok)?.value?.comments ?: it.comments) }
    }

    /** A toggle: a second tap while the first is on its way would undo it, so it is ignored. */
    fun like() {
        if (liking) return
        val post = (_state.value.post as? Load.Ready)?.value ?: return
        liking = true
        viewModelScope.launch {
            try {
                when (val r = repo.toggleLike(id)) {
                    is ApiResult.Ok -> _state.update { it.copy(post = Load.Ready(post.copy(likedByMe = r.value.liked, likeCount = r.value.likeCount))) }
                    is ApiResult.Err -> _state.update { it.copy(error = r.message) }
                }
            } finally {
                liking = false
            }
        }
    }

    fun comment(text: String, onDone: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(busy = true, error = null) }
        when (val r = repo.addComment(id, text)) {
            is ApiResult.Ok -> { _state.update { it.copy(busy = false, comments = listOf(r.value) + it.comments) }; onDone() }
            is ApiResult.Err -> _state.update { it.copy(busy = false, error = r.message) }
        }
    }

    fun delete(commentId: String) = viewModelScope.launch {
        if (repo.deleteComment(commentId) is ApiResult.Ok) _state.update { it.copy(comments = it.comments.filterNot { c -> c.id == commentId }) }
    }

    fun loadMore() = viewModelScope.launch {
        val more = repo.blogComments(id, _state.value.comments.size)
        if (more is ApiResult.Ok) _state.update { it.copy(comments = it.comments + more.value.filterNot { m -> it.comments.any { c -> c.id == m.id } }) }
    }
}

@Composable
fun BlogPostScreen(id: String, onBack: () -> Unit, onSignIn: () -> Unit) {
    val vm: BlogPostViewModel = viewModel(key = "post-$id", factory = vmFactory { BlogPostViewModel(it.repo, it.auth, id) })
    val state by vm.state.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf("") }

    Scaffold(topBar = { BackTopBar("", onBack) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoadContent(state.post, vm::load) { post ->
                LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(bottom = 32.dp)) {
                    item {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Text(post.title, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(10.dp))
                            GoldRule()
                            Spacer(Modifier.height(10.dp))
                            Text(listOfNotNull(post.createdBy?.name, formatDate(post.createdAt)).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = Brand.gold)
                        }
                    }
                    itemsIndexed(post.images, key = { i, img -> "$i:${img.imagePath}" }) { _, img -> // a post can repeat an image
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            RemoteImage(img.imagePath, img.caption, Modifier.fillMaxWidth().aspectRatio(1.5f).clip(MaterialTheme.shapes.large))
                            img.caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                        }
                    }
                    item {
                        Column(Modifier.padding(20.dp)) {
                            post.excerpt?.takeIf { it.isNotBlank() }?.let {
                                Text(rememberPlainText(it), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                            }
                            HtmlText(post.body, serif = true)
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton({ if (session == null) onSignIn() else vm.like() }) {
                                    Icon(if (post.likedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like", tint = if (post.likedByMe) Brand.danger else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${post.likeCount}", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.width(16.dp))
                                Text(countOf(maxOf(post.commentCount, state.comments.size), "comment"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Text("Comments", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(10.dp))
                            state.error?.let { Banner(it, Tone.Danger); Spacer(Modifier.height(8.dp)) }
                            if (session == null) {
                                SecondaryButton("Sign in to comment", onSignIn)
                            } else {
                                OutlinedTextField(
                                    draft, { draft = it.take(2000) }, Modifier.fillMaxWidth(),
                                    placeholder = { Text("Write a comment") },
                                    trailingIcon = { IconButton({ vm.comment(draft.trim()) { draft = "" } }, enabled = draft.isNotBlank() && !state.busy) { Icon(Icons.AutoMirrored.Rounded.Send, "Post") } },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    items(state.comments, key = { it.id }) { c ->
                        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Avatar(c.user?.name, c.user?.image, 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.user?.name ?: "Student", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.width(8.dp))
                                    Text(relativeTime(c.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(c.content, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (c.user?.id != null && c.user.id == session?.user?.id) {
                                IconButton({ vm.delete(c.id) }) { Icon(Icons.Rounded.Delete, "Delete comment", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                    if (state.comments.size < post.commentCount) item { TextButton(vm::loadMore, Modifier.padding(horizontal = 12.dp)) { Text("Load more comments") } }
                }
            }
        }
    }
}

// ================================================================= fatwa

@Composable
fun FatwaListScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onAsk: () -> Unit, categories: List<String>) {
    val vm: PagedViewModel<Fatwa> = viewModel(key = "fatwa", factory = vmFactory { c -> PagedViewModel { p, q, f -> c.repo.fatwas(p, q, f) } })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { BackTopBar("Fatwa", onBack) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onAsk, icon = { Icon(Icons.Rounded.Edit, null) }, text = { Text("Ask a question") }) },
    ) { padding ->
        PagedListBody(vm, "Search questions", "No answered questions yet", { it.id }, Modifier.padding(padding), header = {
            if (categories.isNotEmpty()) item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(state.filter == null, { vm.filter(null) }, { Text("All") })
                    categories.forEach { c -> FilterChip(state.filter == c, { vm.filter(c) }, { Text(c) }) }
                }
            }
        }) { FatwaRow(it) { onOpen(it.id) } }
    }
}

@Composable
fun FatwaDetailScreen(id: String, onBack: () -> Unit) {
    val vm: SimpleLoadViewModel<Fatwa> = viewModel(key = "fatwa-$id", factory = vmFactory { c -> SimpleLoadViewModel { c.repo.fatwa(id) } })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Fatwa", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { f ->
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Overline(f.category)
                    Spacer(Modifier.height(6.dp))
                    Text(f.title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    AppCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
                        Overline("Question", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text(f.question, style = MaterialTheme.typography.bodyLarge)
                        if (!f.askerName.isNullOrBlank() && f.askerName != "Anonymous") {
                            Spacer(Modifier.height(8.dp))
                            Text("Asked by ${f.askerName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Mosque, null, tint = Brand.gold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Answer", style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(4.dp))
                    f.answeredAt?.let { Text(formatDate(it), style = MaterialTheme.typography.labelMedium, color = Brand.gold) }
                    Spacer(Modifier.height(12.dp))
                    HtmlText(f.answer ?: "", serif = true)
                }
            }
        }
    }
}

class AskFatwaViewModel(private val repo: AcademyRepository) : ViewModel() {
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val done = MutableStateFlow(false)
    fun submit(body: AskFatwaRequest) = viewModelScope.launch {
        busy.value = true; error.value = null
        when (val r = repo.askFatwa(body)) {
            is ApiResult.Ok -> done.value = true
            is ApiResult.Err -> error.value = r.message
        }
        busy.value = false
    }
}

@Composable
fun AskFatwaScreen(onBack: () -> Unit, defaultName: String?, defaultEmail: String?, categories: List<String>) {
    val vm: AskFatwaViewModel = viewModel(factory = vmFactory { AskFatwaViewModel(it.repo) })
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var question by rememberSaveable { mutableStateOf("") }
    // Shown and picked from the same list: indexing the (possibly empty) server list crashed when it hadn't loaded.
    val options = categories.ifEmpty { listOf("General") }
    var category by rememberSaveable { mutableStateOf(options.first()) }
    LaunchedEffect(options) { if (category !in options) category = options.first() } // the real list arrived late
    var name by rememberSaveable { mutableStateOf(defaultName ?: "") }
    var email by rememberSaveable { mutableStateOf(defaultEmail ?: "") }

    Scaffold(topBar = { BackTopBar("Ask a question", onBack) }) { padding ->
        if (done) {
            Box(Modifier.padding(padding)) { MessageState(Icons.Rounded.QuestionAnswer, "Question sent", "A scholar will answer it. You'll get an e-mail when the answer is published.", "Done", onBack) }
            return@Scaffold
        }
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Your question is answered by the academy's scholars and may be published without your e-mail. Choose \"Anonymous\" as your name to hide it too.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            error?.let { Banner(it, Tone.Danger) }
            OutlinedTextField(title, { title = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Short title") }, singleLine = true)
            Dropdown(category, options, Modifier.fillMaxWidth(), label = "Category") { category = options[it] }
            OutlinedTextField(question, { question = it.take(5000) }, Modifier.fillMaxWidth().height(180.dp), label = { Text("Your question") })
            OutlinedTextField(name, { name = it.take(100) }, Modifier.fillMaxWidth(), label = { Text("Your name") }, singleLine = true)
            OutlinedTextField(email, { email = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Your e-mail (not published)") }, singleLine = true)
            PrimaryButton("Send question", { vm.submit(AskFatwaRequest(title.trim(), question.trim(), category, name.trim(), email)) },
                enabled = title.length >= 5 && question.length >= 20 && name.isNotBlank() && email.contains("@"), loading = busy)
        }
    }
}

// =============================================================== library

@Composable
fun LibraryListScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm: PagedViewModel<LibraryItem> = viewModel(key = "library", factory = vmFactory { c -> PagedViewModel { p, q, _ -> c.repo.library(p, q) } })
    Scaffold(topBar = { BackTopBar("Digital library", onBack) }) { padding ->
        PagedListBody(vm, "Search books", "The library is empty", { it.id }, Modifier.padding(padding)) { item ->
            AppCard(Modifier.fillMaxWidth(), onClick = { onOpen(item.id) }, contentPadding = PaddingValues(12.dp)) {
                Row {
                    RemoteImage(item.imagePath, item.title, Modifier.size(width = 64.dp, height = 88.dp).clip(MaterialTheme.shapes.small))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Overline(item.topic)
                        Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(item.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(listOf(item.level, item.language).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (item.pdfUrl != null) Icon(Icons.Rounded.PictureAsPdf, "PDF available", tint = Brand.gold)
                }
            }
        }
    }
}

@Composable
fun LibraryItemScreen(id: String, onBack: () -> Unit) {
    val vm: SimpleLoadViewModel<LibraryItem> = viewModel(key = "lib-$id", factory = vmFactory { c -> SimpleLoadViewModel { c.repo.libraryItem(id) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(topBar = { BackTopBar("Library", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { item ->
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RemoteImage(item.imagePath, item.title, Modifier.width(180.dp).aspectRatio(0.72f).clip(MaterialTheme.shapes.large))
                    Spacer(Modifier.height(20.dp))
                    Overline(item.topic)
                    Text(item.title, style = MaterialTheme.typography.headlineSmall)
                    Text(item.author, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    AppCard(Modifier.fillMaxWidth()) {
                        DetailLine("Level", item.level); DetailLine("Language", item.language)
                    }
                    Spacer(Modifier.height(20.dp))
                    if (item.pdfUrl != null) PrimaryButton("Read the PDF", { openWeb(context, item.pdfUrl) })
                    else Banner("This book is available at the academy's library.", Tone.Info, icon = Icons.Rounded.LibraryBooks)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

// ========================================================= announcements

@Composable
fun AnnouncementsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm: PagedViewModel<SiteAnnouncement> = viewModel(key = "announcements", factory = vmFactory { c -> PagedViewModel { p, q, _ -> c.repo.announcements(p, q) } })
    Scaffold(topBar = { BackTopBar("Announcements", onBack) }) { padding ->
        PagedListBody(vm, null, "No announcements", { it.id }, Modifier.padding(padding)) { a ->
            AppCard(Modifier.fillMaxWidth(), onClick = { onOpen(a.id) }, contentPadding = PaddingValues(0.dp)) {
                a.images.firstOrNull()?.let { RemoteImage(it.imagePath, null, Modifier.fillMaxWidth().height(150.dp)) }
                Column(Modifier.padding(16.dp)) {
                    Overline(a.eventDate?.let { formatDate(it) } ?: formatDate(a.createdAt))
                    Spacer(Modifier.height(4.dp))
                    Text(a.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(rememberPlainText(a.body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun AnnouncementScreen(id: String, onBack: () -> Unit) {
    val vm: SimpleLoadViewModel<SiteAnnouncement> = viewModel(key = "ann-$id", factory = vmFactory { c -> SimpleLoadViewModel { c.repo.announcement(id) } })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Announcement", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { a ->
                Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(a.title, style = MaterialTheme.typography.headlineSmall)
                    GoldRule()
                    if (a.eventDate != null || a.location != null) {
                        AppCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                            a.eventDate?.let { Text("📅  ${formatDate(it)}", style = MaterialTheme.typography.titleSmall) }
                            a.location?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.LocationOn, null, Modifier.size(16.dp), tint = Brand.gold); Spacer(Modifier.width(6.dp)); Text(it, style = MaterialTheme.typography.titleSmall) }
                            }
                        }
                    }
                    a.images.forEach { RemoteImage(it.imagePath, it.caption, Modifier.fillMaxWidth().aspectRatio(1.5f).clip(MaterialTheme.shapes.large)) }
                    HtmlText(a.body)
                    Text("Posted ${formatDate(a.createdAt)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ============================================================== teachers

@Composable
fun TeachersScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val vm: PagedViewModel<Teacher> = viewModel(key = "teachers", factory = vmFactory { c -> PagedViewModel { p, q, _ -> c.repo.teachers(p, q) } })
    Scaffold(topBar = { BackTopBar("Our teachers", onBack) }) { padding ->
        PagedListBody(vm, "Search teachers", "No teachers listed", { it.id }, Modifier.padding(padding)) { t ->
            AppCard(Modifier.fillMaxWidth(), onClick = { onOpen(t.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(t.name, t.imageUrl, 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.name, style = MaterialTheme.typography.titleLarge)
                        Text(t.specialization, style = MaterialTheme.typography.bodySmall, color = Brand.gold)
                        Spacer(Modifier.height(4.dp))
                        Text(t.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherScreen(id: String, onBack: () -> Unit, onOpenCourse: (String) -> Unit) {
    val vm: SimpleLoadViewModel<TeacherDetailResponse> = viewModel(key = "teacher-$id", factory = vmFactory { c -> SimpleLoadViewModel { c.repo.teacher(id) } })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { d ->
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Avatar(d.teacher.name, d.teacher.imageUrl, 96.dp)
                            Spacer(Modifier.height(12.dp))
                            Text(d.teacher.name, style = MaterialTheme.typography.headlineSmall)
                            Text(d.teacher.specialization, style = MaterialTheme.typography.bodyMedium, color = Brand.gold)
                            Spacer(Modifier.height(14.dp))
                            Text(d.teacher.bio, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (d.courses.isNotEmpty()) {
                        item { Spacer(Modifier.height(8.dp)); Text("Courses", style = MaterialTheme.typography.titleLarge) }
                        items(d.courses, key = { it.id }) { CourseCard(it) { onOpenCourse(it.id) } }
                    }
                }
            }
        }
    }
}

/** One-shot loader for detail screens. */
class SimpleLoadViewModel<T>(private val fetch: suspend () -> ApiResult<T>) : ViewModel() {
    private val _state = MutableStateFlow<Load<T>>(Load.Loading)
    val state: StateFlow<Load<T>> = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch { _state.value = Load.Loading; _state.value = fetch().toLoad() }
}
