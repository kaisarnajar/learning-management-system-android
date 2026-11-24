package com.darsequran.academy.ui.account

import com.darsequran.academy.ui.theme.HeroUnderStatusBar
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.BuildConfig
import com.darsequran.academy.R
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.SessionStore
import com.darsequran.academy.data.remote.AppConfig
import com.darsequran.academy.data.remote.ContactRequest
import com.darsequran.academy.data.remote.Enrollment
import com.darsequran.academy.data.remote.Notification
import com.darsequran.academy.data.remote.PaymentHistory
import com.darsequran.academy.data.remote.Profile
import com.darsequran.academy.data.remote.ProfileUpdate
import com.darsequran.academy.data.remote.Review
import com.darsequran.academy.data.remote.ReviewRequest
import com.darsequran.academy.data.remote.WaiverRequest
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.data.repo.AuthRepository
import com.darsequran.academy.data.repo.orNull
import com.darsequran.academy.ui.auth.PasswordField
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Avatar
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.EmptyState
import com.darsequran.academy.ui.common.HeroBox
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadContent
import com.darsequran.academy.ui.common.LoadingBox
import com.darsequran.academy.ui.common.MessageState
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PagedListBody
import com.darsequran.academy.ui.common.PagedViewModel
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.openBytes
import com.darsequran.academy.ui.common.openWeb
import com.darsequran.academy.ui.common.relativeTime
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.ui.common.titleCase
import com.darsequran.academy.ui.common.toLoad
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.explore.SimpleLoadViewModel
import com.darsequran.academy.ui.pay.ChoicePill
import com.darsequran.academy.ui.pay.Dropdown
import com.darsequran.academy.ui.pay.readImage
import com.darsequran.academy.ui.theme.Brand
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ================================================================ account

class AccountViewModel(private val auth: AuthRepository, private val repo: AcademyRepository, private val sessionStore: SessionStore) : ViewModel() {
    val session get() = auth.session
    val theme get() = sessionStore.theme
    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()
    val config = MutableStateFlow<AppConfig?>(null)

    init {
        viewModelScope.launch { auth.session.collect { if (it != null) refresh() else _profile.value = null } }
        viewModelScope.launch { config.value = repo.appConfig().orNull() }
    }

    fun refresh() = viewModelScope.launch { _profile.value = auth.profile().orNull() ?: _profile.value }
    fun signOut() = viewModelScope.launch { auth.logout() }
    fun setTheme(value: String) = viewModelScope.launch { sessionStore.setTheme(value) }
}

@Composable
fun AccountScreen(
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    onEditProfile: () -> Unit,
    onPayments: () -> Unit,
    onWaivers: () -> Unit,
    onReviews: () -> Unit,
    onOrders: () -> Unit,
    onNotifications: () -> Unit,
    onChangePassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    onContact: () -> Unit,
    onAbout: () -> Unit,
    vm: AccountViewModel = viewModel(factory = vmFactory { AccountViewModel(it.auth, it.repo, it.sessionStore) }),
) {
    val session by vm.session.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val config by vm.config.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var themeDialog by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            HeroUnderStatusBar()
            HeroBox(Modifier.fillMaxWidth()) {
                Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(20.dp).padding(top = 8.dp)) {
                    if (session == null) {
                        Image(painterResource(R.drawable.logo), null, Modifier.size(64.dp).clip(CircleShape).background(Color.White))
                        Spacer(Modifier.height(14.dp))
                        Text("Your academy account", style = MaterialTheme.typography.headlineSmall, color = Brand.onHero)
                        Text("Sign in to apply for courses, pay fees and follow your progress.", style = MaterialTheme.typography.bodyMedium, color = Brand.onHero.copy(alpha = 0.85f))
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            androidx.compose.material3.Button(onSignIn, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Brand.goldSoft, contentColor = Color.Black)) { Text("Sign in") }
                            androidx.compose.material3.OutlinedButton(onRegister) { Text("Create account", color = Brand.onHero) }
                        }
                    } else {
                        val user = session!!.user
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(profile?.name ?: user.name, profile?.image ?: user.image, 64.dp)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(profile?.name ?: user.name ?: "Student", style = MaterialTheme.typography.headlineSmall, color = Brand.onHero, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(user.email, style = MaterialTheme.typography.bodySmall, color = Brand.onHero.copy(alpha = 0.8f))
                                (profile?.registrationNumber ?: user.registrationNumber)?.let {
                                    Spacer(Modifier.height(6.dp))
                                    Text(it, style = MaterialTheme.typography.labelLarge, color = Brand.goldSoft)
                                }
                            }
                        }
                        if (profile?.profileComplete == false) {
                            Spacer(Modifier.height(14.dp))
                            Banner("Complete your profile to enrol in courses.", Tone.Warning, Modifier.clickable(onClick = onEditProfile))
                        }
                    }
                }
            }
        }

        if (session != null) {
            item { Group("My learning") }
            item { Entry(Icons.Rounded.Notifications, "Notifications", onNotifications) }
            item { Entry(Icons.Rounded.Payments, "Payments & receipts", onPayments) }
            item { Entry(Icons.Rounded.RequestQuote, "Fee waiver requests", onWaivers) }
            item { Entry(Icons.Rounded.Star, "My reviews", onReviews) }
            item { Entry(Icons.Rounded.ShoppingBag, "Book orders", onOrders) }
            item { Group("Settings") }
            item { Entry(Icons.Rounded.Edit, "Edit profile", onEditProfile) }
            if (profile?.hasPassword != false) item { Entry(Icons.Rounded.Key, "Change password", onChangePassword) }
        } else item { Group("Settings") }
        item { Entry(Icons.Rounded.Contrast, "Appearance", { themeDialog = true }, trailing = when (theme) { "light" -> "Light"; "dark" -> "Dark"; else -> "System" }) }
        item { Group("Academy") }
        item { Entry(Icons.Rounded.SupportAgent, "Contact us", onContact) }
        item { Entry(Icons.Rounded.Info, "About the academy", onAbout) }
        item { Entry(Icons.Rounded.Policy, "Privacy policy", { openWeb(context, config?.policies?.privacyPolicyUrl ?: "/privacy-policy") }) }
        item { Entry(Icons.Rounded.Gavel, "Terms & conditions", { openWeb(context, config?.policies?.termsUrl ?: "/terms-and-conditions") }) }
        if (session != null) {
            item { Group("") }
            item { Entry(Icons.AutoMirrored.Rounded.Logout, "Sign out", { confirmSignOut = true }) }
            item { Entry(Icons.Rounded.DeleteForever, "Delete my account", onDeleteAccount, danger = true) }
        }
        item {
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
        }
    }

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("Appearance") },
            text = {
                Column {
                    listOf("system" to "Follow the phone", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                        Row(Modifier.fillMaxWidth().clickable { vm.setTheme(value); themeDialog = false }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(theme == value, { vm.setTheme(value); themeDialog = false }); Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ themeDialog = false }) { Text("Close") } },
        )
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You can sign in again at any time.") },
            confirmButton = { TextButton({ confirmSignOut = false; vm.signOut() }) { Text("Sign out") } },
            dismissButton = { TextButton({ confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Group(title: String) {
    Spacer(Modifier.height(18.dp))
    if (title.isNotBlank()) Overline(title, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
}

@Composable
private fun Entry(icon: ImageVector, label: String, onClick: () -> Unit, trailing: String? = null, danger: Boolean = false) {
    val color = if (danger) Brand.danger else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (danger) Brand.danger else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(8.dp)) }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

// ============================================================ edit profile

data class EditState(val profile: Load<Profile> = Load.Loading, val config: AppConfig? = null, val saving: Boolean = false, val error: String? = null, val saved: Boolean = false, val photoBusy: Boolean = false)

class EditProfileViewModel(private val auth: AuthRepository, private val repo: AcademyRepository) : ViewModel() {
    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch {
        val config = async { repo.appConfig().orNull() }
        _state.update { it.copy(profile = auth.profile().toLoad(), config = config.await()) }
    }
    fun save(update: ProfileUpdate) = viewModelScope.launch {
        _state.update { it.copy(saving = true, error = null, saved = false) }
        when (val r = auth.updateProfile(update)) {
            is ApiResult.Ok -> _state.update { it.copy(saving = false, saved = true, profile = Load.Ready(r.value)) }
            is ApiResult.Err -> _state.update { it.copy(saving = false, error = r.message) }
        }
    }
    fun photo(bytes: ByteArray, mime: String) = viewModelScope.launch {
        _state.update { it.copy(photoBusy = true, error = null) }
        when (val r = auth.uploadPhoto(bytes, mime)) {
            is ApiResult.Ok -> load()
            is ApiResult.Err -> _state.update { it.copy(error = r.message) }
        }
        _state.update { it.copy(photoBusy = false) }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val vm: EditProfileViewModel = viewModel(factory = vmFactory { EditProfileViewModel(it.auth, it.repo) })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Edit profile", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state.profile, vm::load) { p -> ProfileForm(p, state, vm) }
        }
    }
}

@Composable
private fun ProfileForm(p: Profile, state: EditState, vm: EditProfileViewModel) {
    val context = LocalContext.current
    val config = state.config
    val countries = config?.lookups?.countries.orEmpty()
    val occupations = config?.lookups?.occupations.orEmpty()
    var name by rememberSaveable { mutableStateOf(p.name ?: "") }
    var father by rememberSaveable { mutableStateOf(p.fatherName ?: "") }
    var dob by rememberSaveable { mutableStateOf(p.dateOfBirth?.take(10) ?: "") }
    var occupation by rememberSaveable { mutableStateOf(p.occupation ?: "") }
    var address by rememberSaveable { mutableStateOf(p.address ?: "") }
    var gender by rememberSaveable { mutableStateOf(p.gender ?: "") }
    var country by rememberSaveable { mutableStateOf(p.whatsappCountry ?: config?.lookups?.defaultCountryCode ?: "IN") }
    var whatsapp by rememberSaveable { mutableStateOf(p.whatsappLocal ?: "") }
    var picked by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked = it }
    LaunchedEffect(picked) { picked?.let { uri -> readImage(context, uri)?.let { (b, m) -> vm.photo(b, m) } } }
    val selectedCountry = countries.firstOrNull { it.code == country }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.align(Alignment.CenterHorizontally)) {
            Avatar(p.name, p.image, 96.dp)
            if (gender != "FEMALE") {
                IconButton(
                    { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                ) { if (state.photoBusy) LoadingBox(Modifier.size(18.dp)) else Icon(Icons.Rounded.CameraAlt, "Change photo", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp)) }
            }
        }
        if (!p.profileComplete) Banner("Fill in every field to complete your profile; you get your registration number when it is complete.", Tone.Info)
        state.error?.let { Banner(it, Tone.Danger) }
        if (state.saved) Banner("Profile saved.", Tone.Success)
        Field(name, "Full name") { name = it }
        Field(father, "Father's name") { father = it }
        Field(dob, "Date of birth (YYYY-MM-DD)", KeyboardType.Number) { v -> dob = v.filter { it.isDigit() || it == '-' }.take(10) }
        Text("Gender", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoicePill("Male", gender == "MALE", Modifier.weight(1f)) { gender = "MALE" }
            ChoicePill("Female", gender == "FEMALE", Modifier.weight(1f)) { gender = "FEMALE" }
        }
        if (occupations.isNotEmpty()) Dropdown(occupations.firstOrNull { it.value == occupation }?.label ?: "Select", occupations.map { it.label }, Modifier.fillMaxWidth(), "Occupation") { occupation = occupations[it].value }
        Field(address, "Address", singleLine = false) { address = it }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (countries.isNotEmpty()) Dropdown(selectedCountry?.let { "${it.flag} +${it.dialCode}" } ?: country, countries.map { "${it.flag} ${it.name} (+${it.dialCode})" }, Modifier.weight(1f), "Country") { country = countries[it].code }
            OutlinedTextField(whatsapp, { v -> whatsapp = v.filter(Char::isDigit).take(selectedCountry?.localNumberMaxLength ?: 15) }, Modifier.weight(1.4f), label = { Text("WhatsApp") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        }
        PrimaryButton("Save", {
            vm.save(ProfileUpdate(
                name = name.trim().ifBlank { null }, fatherName = father.trim().ifBlank { null }, dateOfBirth = dob.ifBlank { null },
                occupation = occupation.ifBlank { null }, address = address.trim().ifBlank { null }, gender = gender.ifBlank { null },
                country = if (whatsapp.isNotBlank()) country else null, whatsapp = whatsapp.ifBlank { null },
            ))
        }, loading = state.saving)
    }
}

@Composable
private fun Field(value: String, label: String, keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = singleLine, keyboardOptions = KeyboardOptions(keyboardType = keyboard))
}

// =============================================================== payments

@Composable
fun rememberReceiptOpener(): (String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { (context.applicationContext as com.darsequran.academy.AcademyApp).container.repo }
    return { id ->
        android.widget.Toast.makeText(context, "Preparing your receipt…", android.widget.Toast.LENGTH_SHORT).show()
        scope.launch {
            when (val r = repo.receipt(id)) {
                is ApiResult.Ok -> openBytes(context, r.value, "receipt-$id.pdf", "application/pdf")
                is ApiResult.Err -> android.widget.Toast.makeText(context, r.message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun PaymentsScreen(onBack: () -> Unit) {
    val vm: SimpleLoadViewModel<Pair<PaymentHistory, List<Enrollment>>> = viewModel(key = "payments", factory = vmFactory { c ->
        SimpleLoadViewModel<Pair<PaymentHistory, List<Enrollment>>> {
            when (val h = c.repo.paymentHistory()) {
                is ApiResult.Ok -> ApiResult.Ok(h.value to (c.repo.enrollments().orNull() ?: emptyList()))
                is ApiResult.Err -> h
            }
        }
    })
    val state by vm.state.collectAsStateWithLifecycle()
    val openReceipt = rememberReceiptOpener()
    Scaffold(topBar = { BackTopBar("Payments & receipts", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { (history, enrollments) ->
                val titles = enrollments.associate { it.courseId to (it.course?.title ?: "") }
                val records = history.records.associateBy { it.id }
                if (history.submissions.isEmpty() && history.records.isEmpty()) EmptyState("No payments yet", "Fees you pay in the app or on the website appear here.")
                else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(history.submissions.sortedByDescending { it.createdAt }, key = { it.id }) { s ->
                        val (label, tone) = statusLabel(s.status)
                        val record = s.paymentRecordId?.let { records[it] }
                        AppCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    titles[s.courseId]?.takeIf { it.isNotBlank() }?.let { Overline(it) }
                                    Text(s.label, style = MaterialTheme.typography.titleMedium)
                                    Text("${formatDate(s.createdAt)}${s.upiTransactionId?.takeIf { it.isNotBlank() && it != "FEE-WAIVER" }?.let { " · Ref $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(rupees(s.amountInrPaise), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(4.dp)); StatusChip(label, tone)
                                }
                            }
                            if (record?.receiptGeneratedAt != null) {
                                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Brand.outlineSoft)
                                Row(Modifier.fillMaxWidth().clickable { openReceipt(record.id) }, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Receipt ${record.invoiceNumber ?: ""}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    val recordsWithoutSubmission = history.records.filter { r -> history.submissions.none { it.paymentRecordId == r.id } }
                    if (recordsWithoutSubmission.isNotEmpty()) item { Spacer(Modifier.height(8.dp)); Overline("Other payments recorded by the academy") }
                    items(recordsWithoutSubmission, key = { "r" + it.id }) { r ->
                        AppCard(Modifier.fillMaxWidth(), onClick = if (r.receiptGeneratedAt != null) ({ openReceipt(r.id) }) else null) {
                            Row {
                                Column(Modifier.weight(1f)) {
                                    Text(r.description ?: titleCase(r.paymentType ?: "Payment"), style = MaterialTheme.typography.titleMedium)
                                    Text(formatDate(r.paidAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(rupees(r.amountInrPaise), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            if (r.receiptGeneratedAt != null) Text("Tap for the receipt", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// ================================================================ waivers

class WaiversViewModel(private val repo: AcademyRepository) : ViewModel() {
    val state = MutableStateFlow<Load<Pair<List<WaiverRequest>, List<Enrollment>>>>(Load.Loading)
    val sending = MutableStateFlow(false)
    val message = MutableStateFlow<Pair<String, Boolean>?>(null)
    init { load() }
    fun load() = viewModelScope.launch {
        val e = async { repo.enrollments().orNull() ?: emptyList() }
        state.value = when (val r = repo.waiverRequests()) { is ApiResult.Ok -> Load.Ready(r.value to e.await()); is ApiResult.Err -> Load.Failed(r.message) }
    }
    fun request(courseId: String, feeType: String, reason: String, onDone: () -> Unit) = viewModelScope.launch {
        sending.value = true
        when (val r = repo.requestWaiver(courseId, feeType, reason)) {
            is ApiResult.Ok -> { message.value = "Request sent. The academy will review it." to true; onDone(); load() }
            is ApiResult.Err -> message.value = r.message to false
        }
        sending.value = false
    }
}

@Composable
fun WaiversScreen(onBack: () -> Unit) {
    val vm: WaiversViewModel = viewModel(factory = vmFactory { WaiversViewModel(it.repo) })
    val state by vm.state.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var showForm by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = { BackTopBar("Fee waivers", onBack) },
        floatingActionButton = {
            if (!showForm) ExtendedFloatingActionButton(onClick = { showForm = true }, icon = { Icon(Icons.Rounded.Edit, null) }, text = { Text("Request a waiver") })
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { (requests, enrollments) ->
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    message?.let { (text, ok) -> item { Banner(text, if (ok) Tone.Success else Tone.Danger) } }
                    if (showForm) item { WaiverForm(enrollments, sending, { showForm = false }) { c, t, r -> vm.request(c, t, r) { showForm = false } } }
                    if (requests.isEmpty() && !showForm) item { EmptyState("No waiver requests", "If paying the fee is difficult, you can ask the academy for a full or partial waiver.") }
                    items(requests, key = { it.id }) { w ->
                        val (label, tone) = statusLabel(w.status)
                        AppCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(w.course?.title ?: "Course", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                StatusChip(label, tone)
                            }
                            Text("${titleCase(w.feeType ?: "BOTH")} fee · ${formatDate(w.createdAt)}", style = MaterialTheme.typography.labelMedium, color = Brand.gold)
                            Spacer(Modifier.height(6.dp))
                            Text(w.reason, style = MaterialTheme.typography.bodyMedium)
                            if (w.status == "APPROVED") { Spacer(Modifier.height(6.dp)); Text("Your waiver appears as an option when you pay this fee.", style = MaterialTheme.typography.bodySmall, color = Brand.success) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaiverForm(enrollments: List<Enrollment>, sending: Boolean, onCancel: () -> Unit, onSend: (String, String, String) -> Unit) {
    val courses = enrollments.mapNotNull { e -> e.course?.let { e.courseId to it.title } }
    var course by rememberSaveable { mutableStateOf(courses.firstOrNull()?.first ?: "") }
    var type by rememberSaveable { mutableStateOf("both") }
    var reason by rememberSaveable { mutableStateOf("") }
    AppCard(Modifier.fillMaxWidth()) {
        Text("Request a waiver", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        if (courses.isEmpty()) { Text("Apply for a course first; waivers are requested per course.", style = MaterialTheme.typography.bodyMedium); TextButton(onCancel) { Text("Close") }; return@AppCard }
        Dropdown(courses.firstOrNull { it.first == course }?.second ?: "Select", courses.map { it.second }, Modifier.fillMaxWidth(), "Course") { course = courses[it].first }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("enrollment" to "Enrollment", "course" to "Course fee", "both" to "Both").forEach { (v, l) -> ChoicePill(l, type == v, Modifier.weight(1f)) { type = v } }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(reason, { reason = it.take(1000) }, Modifier.fillMaxWidth().height(130.dp), label = { Text("Why do you need a waiver?") }, supportingText = { Text("${reason.length}/1000 · at least 10 characters") })
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onCancel, Modifier.weight(1f)) { Text("Cancel") }
            PrimaryButton("Send", { onSend(course, type, reason.trim()) }, Modifier.weight(2f), enabled = reason.trim().length >= 10 && course.isNotBlank(), loading = sending)
        }
    }
}

// ================================================================ reviews

class ReviewsViewModel(private val repo: AcademyRepository) : ViewModel() {
    val state = MutableStateFlow<Load<List<Review>>>(Load.Loading)
    val sending = MutableStateFlow(false)
    val message = MutableStateFlow<Pair<String, Boolean>?>(null)
    init { load() }
    fun load() = viewModelScope.launch { state.value = repo.myReviews().toLoad() }
    fun send(body: ReviewRequest, onDone: () -> Unit) = viewModelScope.launch {
        sending.value = true
        when (val r = repo.addReview(body)) {
            is ApiResult.Ok -> { message.value = "Thank you! Your review will appear once the academy approves it." to true; onDone(); load() }
            is ApiResult.Err -> message.value = r.message to false
        }
        sending.value = false
    }
}

@Composable
fun ReviewsScreen(onBack: () -> Unit) {
    val vm: ReviewsViewModel = viewModel(factory = vmFactory { ReviewsViewModel(it.repo) })
    val state by vm.state.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var rating by rememberSaveable { mutableStateOf(5) }
    var quote by rememberSaveable { mutableStateOf("") }
    var course by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { BackTopBar("My reviews", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { reviews ->
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        AppCard(Modifier.fillMaxWidth()) {
                            Text("Share your experience", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Row { (1..5).forEach { i -> IconButton({ rating = i }) { Icon(if (i <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder, "$i stars", tint = Brand.goldSoft) } } }
                            OutlinedTextField(quote, { quote = it.take(600) }, Modifier.fillMaxWidth().height(120.dp), label = { Text("Your review") })
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(course, { course = it.take(100) }, Modifier.weight(1f), label = { Text("Course (optional)") }, singleLine = true)
                                OutlinedTextField(location, { location = it.take(100) }, Modifier.weight(1f), label = { Text("City (optional)") }, singleLine = true)
                            }
                            Spacer(Modifier.height(10.dp))
                            message?.let { (t, ok) -> Banner(t, if (ok) Tone.Success else Tone.Danger); Spacer(Modifier.height(8.dp)) }
                            PrimaryButton("Submit review", { vm.send(ReviewRequest(quote.trim(), rating, course.ifBlank { null }, location.ifBlank { null })) { quote = ""; course = ""; location = "" } }, enabled = quote.trim().length >= 20, loading = sending)
                        }
                    }
                    items(reviews, key = { it.id }) { r ->
                        val (label, tone) = statusLabel(r.status ?: "pending")
                        AppCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(Modifier.weight(1f)) { repeat(r.rating) { Icon(Icons.Rounded.Star, null, Modifier.size(16.dp), tint = Brand.goldSoft) } }
                                StatusChip(label, tone)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("“${r.quote}”", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// ========================================================== notifications

@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpenHref: (String) -> Unit) {
    val vm: PagedViewModel<Notification> = viewModel(key = "notifications", factory = vmFactory { c -> PagedViewModel { p, _, f -> c.repo.notifications(p, f == "unread") } })
    val repo = (LocalContext.current.applicationContext as com.darsequran.academy.AcademyApp).container.repo
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Notifications", onBack, actions = { TextButton({ scope.launch { repo.markAllRead(); vm.refresh() } }) { Text("Mark all read") } }) }) { padding ->
        PagedListBody(vm, null, "You're all caught up", { it.id }, Modifier.padding(padding), header = {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(state.filter == null, { vm.filter(null) }, { Text("All") })
                    androidx.compose.material3.FilterChip(state.filter == "unread", { vm.filter("unread") }, { Text("Unread") })
                }
            }
        }) { n ->
            val unread = n.readAt == null
            AppCard(Modifier.fillMaxWidth(), onClick = { scope.launch { repo.notification(n.id) }; onOpenHref(n.href) },
                color = if (unread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLowest) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unread) { Box(Modifier.size(8.dp).clip(CircleShape).background(Brand.goldSoft)); Spacer(Modifier.width(8.dp)) }
                    Text(n.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(relativeTime(n.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                n.body?.let { Spacer(Modifier.height(4.dp)); Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

// ======================================================== change password

@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { (context.applicationContext as com.darsequran.academy.AcademyApp).container.auth }
    val scope = rememberCoroutineScope()
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    Scaffold(topBar = { BackTopBar("Change password", onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            result?.let { (t, ok) -> Banner(t, if (ok) Tone.Success else Tone.Danger) }
            PasswordField(current, "Current password", {}) { current = it }
            PasswordField(new, "New password (at least 8 characters)", {}) { new = it }
            Text("Other phones signed in to your account will be signed out.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton("Change password", {
                busy = true
                scope.launch {
                    result = when (val r = auth.changePassword(current, new)) {
                        is ApiResult.Ok -> { current = ""; new = ""; "Password changed." to true }
                        is ApiResult.Err -> r.message to false
                    }
                    busy = false
                }
            }, enabled = current.isNotEmpty() && new.length >= 8, loading = busy)
        }
    }
}

// ========================================================= delete account

@Composable
fun DeleteAccountScreen(onBack: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as com.darsequran.academy.AcademyApp).container }
    val scope = rememberCoroutineScope()
    var hasPassword by remember { mutableStateOf<Boolean?>(null) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hasPassword = container.auth.profile().orNull()?.hasPassword ?: true }
    Scaffold(topBar = { BackTopBar("Delete account", onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (done) { MessageState(Icons.Rounded.CheckCircle, "Account deleted", "Your account has been deleted. JazakAllahu khairan for learning with us.", "Close", onDeleted); return@Column }
            Banner("This permanently deletes your account. It cannot be undone.", Tone.Danger, icon = Icons.Rounded.DeleteForever)
            Text("What happens", style = MaterialTheme.typography.titleLarge)
            listOf(
                "Your name, e-mail, photo, contact details and password are removed, and you are signed out everywhere.",
                "Applications still waiting for the academy are withdrawn.",
                "Receipts and fee history are kept without your personal details, because the academy must keep financial records.",
                "You can register again later with the same e-mail address.",
            ).forEach { Row { Text("•  ", color = Brand.gold); Text(it, style = MaterialTheme.typography.bodyMedium) } }
            error?.let { Banner(it, Tone.Danger) }
            when (hasPassword) {
                null -> LoadingBox(Modifier.fillMaxWidth().height(60.dp))
                true -> PasswordField(password, "Your password", {}) { password = it }
                false -> OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), label = { Text("Type DELETE to confirm") }, singleLine = true)
            }
            androidx.compose.material3.Button(
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        when (val r = container.auth.deleteAccount(password.ifBlank { null }, confirm.ifBlank { null })) {
                            is ApiResult.Ok -> { container.cart.clearLocal(); done = true }
                            is ApiResult.Err -> error = r.message
                        }
                        busy = false
                    }
                },
                enabled = !busy && (if (hasPassword == true) password.isNotEmpty() else confirm == "DELETE"),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Brand.danger, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(if (busy) "Deleting…" else "Delete my account") }
        }
    }
}

// =================================================================== contact

@Composable
fun ContactScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as com.darsequran.academy.AcademyApp).container }
    val session by container.sessionStore.session.collectAsStateWithLifecycle()
    var config by remember { mutableStateOf<AppConfig?>(null) }
    LaunchedEffect(Unit) { config = container.repo.appConfig().orNull() }
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(session?.user?.name ?: "") }
    var email by rememberSaveable { mutableStateOf(session?.user?.email ?: "") }
    var phone by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val contact = config?.brand?.contact

    Scaffold(topBar = { BackTopBar("Contact us", onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (contact != null) {
                AppCard(Modifier.fillMaxWidth()) {
                    if (contact.whatsapp.isNotBlank()) ContactLine(Icons.Rounded.Phone, "WhatsApp +${contact.whatsapp}") {
                        val number = contact.whatsapp.filter(Char::isDigit)
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=${Uri.encode(contact.whatsappDefaultMessage)}"))) }
                    }
                    if (contact.phone.isNotBlank()) ContactLine(Icons.Rounded.Phone, contact.phone) { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))) } }
                    if (contact.email.isNotBlank()) ContactLine(Icons.Rounded.Mail, contact.email) { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))) } }
                    if (contact.address.isNotBlank()) ContactLine(Icons.Rounded.Description, contact.address) {}
                }
            }
            Text("Send a message", style = MaterialTheme.typography.titleLarge)
            result?.let { (t, ok) -> Banner(t, if (ok) Tone.Success else Tone.Danger) }
            if (session == null) {
                OutlinedTextField(name, { name = it.take(100) }, Modifier.fillMaxWidth(), label = { Text("Your name") }, singleLine = true)
                OutlinedTextField(email, { email = it.trim() }, Modifier.fillMaxWidth(), label = { Text("Your e-mail") }, singleLine = true)
            }
            OutlinedTextField(phone, { phone = it.take(20) }, Modifier.fillMaxWidth(), label = { Text("Phone (optional)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            OutlinedTextField(message, { message = it.take(2000) }, Modifier.fillMaxWidth().height(160.dp), label = { Text("Message") })
            PrimaryButton("Send", {
                busy = true
                scope.launch {
                    result = when (val r = container.repo.contact(ContactRequest(name.ifBlank { null }, email.ifBlank { null }, phone.ifBlank { null }, message.trim()))) {
                        is ApiResult.Ok -> { message = ""; "Message sent. The academy will reply by e-mail." to true }
                        is ApiResult.Err -> r.message to false
                    }
                    busy = false
                }
            }, enabled = message.trim().length >= 10 && (session != null || (name.isNotBlank() && email.contains("@"))), loading = busy)
        }
    }
}

@Composable
private fun ContactLine(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Brand.gold); Spacer(Modifier.width(12.dp)); Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

// ===================================================================== about

@Composable
fun AboutScreen(onBack: () -> Unit, onTeachers: () -> Unit) {
    val context = LocalContext.current
    Scaffold(topBar = { BackTopBar("About", onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            HeroBox(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.logo), null, Modifier.size(84.dp).clip(CircleShape).background(Color.White))
                    Spacer(Modifier.height(14.dp))
                    Text("Darse Quran Academy", style = MaterialTheme.typography.headlineSmall, color = Brand.onHero)
                    com.darsequran.academy.ui.common.ArabicText("خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ", color = Brand.goldSoft, size = 22)
                    Text("The best among you are those who learn the Quran and teach it.", style = MaterialTheme.typography.bodySmall, color = Brand.onHero.copy(alpha = 0.85f))
                }
            }
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Darse Quran Academy teaches the Quran, Tajweed, Islamic jurisprudence, Hadith and the Arabic language with qualified teachers, online and in person, for students of every age.", style = MaterialTheme.typography.bodyLarge)
                PrimaryButton("Meet our teachers", onTeachers)
                com.darsequran.academy.ui.common.SecondaryButton("Visit the website", { openWeb(context, BuildConfig.BASE_URL + "/about") })
            }
        }
    }
}
