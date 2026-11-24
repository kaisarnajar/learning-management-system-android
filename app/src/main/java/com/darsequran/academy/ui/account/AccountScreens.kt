package com.darsequran.academy.ui.account

import android.content.Intent
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
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.RequestQuote
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.darsequran.academy.data.map
import com.darsequran.academy.data.remote.AppConfig
import com.darsequran.academy.data.remote.Enrollment
import com.darsequran.academy.data.remote.Notification
import com.darsequran.academy.data.remote.PaymentHistory
import com.darsequran.academy.data.remote.PaymentSubmission
import com.darsequran.academy.ui.common.StatusKind
import com.darsequran.academy.data.remote.Profile
import com.darsequran.academy.data.remote.ProfileUpdate
import com.darsequran.academy.data.repo.AcademyRepository
import com.darsequran.academy.data.repo.AuthRepository
import com.darsequran.academy.data.repo.orNull
import com.darsequran.academy.ui.auth.PasswordField
import com.darsequran.academy.ui.common.AppCard
import com.darsequran.academy.ui.common.Avatar
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.EmptyState
import com.darsequran.academy.ui.common.ErrorState
import com.darsequran.academy.ui.common.HeroBox
import com.darsequran.academy.ui.common.Load
import com.darsequran.academy.ui.common.LoadContent
import com.darsequran.academy.ui.common.LoadingBox
import com.darsequran.academy.ui.common.MessageState
import com.darsequran.academy.ui.common.Overline
import com.darsequran.academy.ui.common.PagedListBody
import com.darsequran.academy.ui.common.PagedViewModel
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.ReloadOnReturn
import com.darsequran.academy.ui.common.StatusChip
import com.darsequran.academy.ui.common.SubmitResult
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.formatDate
import com.darsequran.academy.ui.common.openWeb
import com.darsequran.academy.ui.common.relativeTime
import com.darsequran.academy.ui.common.rememberSubmitter
import com.darsequran.academy.ui.common.rupees
import com.darsequran.academy.ui.common.statusLabel
import com.darsequran.academy.ui.common.titleCase
import com.darsequran.academy.ui.common.toLoad
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.common.LoaderViewModel
import com.darsequran.academy.ui.common.ChoicePill
import com.darsequran.academy.ui.common.Dropdown
import com.darsequran.academy.ui.common.readImage
import com.darsequran.academy.ui.theme.Brand
import com.darsequran.academy.ui.theme.HeroUnderStatusBar
import com.darsequran.academy.ui.common.rememberContainer
import com.darsequran.academy.data.remote.Roles
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

    val resend = MutableStateFlow(ResendState())
    fun resendVerification() = viewModelScope.launch {
        val email = _profile.value?.email ?: auth.session.value?.user?.email ?: return@launch
        resend.value = ResendState(sending = true)
        resend.value = resendOutcome(auth.resendVerification(email))
    }
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
    val resend by vm.resend.collectAsStateWithLifecycle()
    ReloadOnReturn { vm.refresh() }
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
                                roleLabel(user.role)?.let {
                                    Spacer(Modifier.height(6.dp))
                                    Text(it, style = MaterialTheme.typography.labelLarge, color = Brand.goldSoft)
                                }
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

        // As on the website's profile pages, an unverified e-mail is flagged with a way to resend the link.
        if (session != null && profile?.let { it.emailVerified == null } == true) {
            item {
                val inset = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp)
                VerificationBanner(resend, { vm.resendVerification() }, inset)
            }
        }
        // Everyone signed in has the same profile pages, as on the website (admins and teachers included).
        val admin = Roles.isAdmin(session?.user?.role)
        if (session != null) {
            item { Group("My learning") }
            item { Entry(Icons.Rounded.Notifications, "Notifications", onNotifications) }
            item { Entry(Icons.Rounded.Payments, "Payments", onPayments) }
            item { Entry(Icons.Rounded.RequestQuote, "Fee waiver requests", onWaivers) }
            item { Entry(Icons.Rounded.Star, "My reviews", onReviews) }
            item { Entry(Icons.Rounded.ShoppingBag, "Book orders", onOrders) }
        }
        if (session != null) {
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
            if (!admin) item { Entry(Icons.Rounded.DeleteForever, "Delete my account", onDeleteAccount, danger = true) }
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

data class EditState(
    val profile: Load<Profile> = Load.Loading,
    val config: AppConfig? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val photoBusy: Boolean = false,
    /** This save completed the profile: the website's "Your profile is complete" step is shown. */
    val justCompleted: Boolean = false,
)

class EditProfileViewModel(private val auth: AuthRepository, private val repo: AcademyRepository) : ViewModel() {
    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state.asStateFlow()
    init { load() }
    fun load() = viewModelScope.launch {
        val config = async { repo.appConfig().orNull() }
        _state.update { it.copy(profile = auth.profile().toLoad(), config = config.await()) }
    }
    /** Saves are one at a time: a second tap before the first answer (even in the same frame) is ignored. */
    fun save(update: ProfileUpdate) {
        if (_state.value.saving) return
        val wasComplete = (_state.value.profile as? Load.Ready)?.value?.profileComplete == true
        _state.update { it.copy(saving = true, error = null, saved = false) }
        viewModelScope.launch {
            when (val r = auth.updateProfile(update)) {
                is ApiResult.Ok -> _state.update {
                    val done = profileJustCompleted(wasComplete, r.value.profileComplete)
                    it.copy(saving = false, saved = true, profile = Load.Ready(r.value), justCompleted = done)
                }
                is ApiResult.Err -> _state.update { it.copy(saving = false, error = r.message) }
            }
        }
    }

    /** One upload at a time. */
    fun photo(bytes: ByteArray, mime: String) {
        if (_state.value.photoBusy) return
        _state.update { it.copy(photoBusy = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.uploadPhoto(bytes, mime)) {
                is ApiResult.Ok -> load()
                is ApiResult.Err -> _state.update { it.copy(error = r.message) }
            }
            _state.update { it.copy(photoBusy = false) }
        }
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit, onBrowseCourses: (() -> Unit)? = null) {
    val vm: EditProfileViewModel = viewModel(factory = vmFactory { EditProfileViewModel(it.auth, it.repo) })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Edit profile", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state.profile, vm::load) { p ->
                if (state.justCompleted) ProfileCompleteStep(onBrowseCourses ?: onBack) else ProfileForm(p, state, vm)
            }
        }
    }
}

@Composable
private fun ProfileForm(p: Profile, state: EditState, vm: EditProfileViewModel) {
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
    val selectedCountry = countries.firstOrNull { it.code == country }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfilePhoto(p, gender, state.photoBusy) { bytes, mime -> vm.photo(bytes, mime) }
        if (!p.profileComplete) Banner("$PROFILE_INCOMPLETE_TITLE. $PROFILE_INCOMPLETE_TEXT", Tone.Warning)
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

/** The website's "Your profile is complete" notice with its "Browse courses" link. */
@Composable
private fun ProfileCompleteStep(onBrowseCourses: () -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AppCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircle, null, tint = Brand.success)
                Spacer(Modifier.width(8.dp))
                Text(PROFILE_COMPLETE_TITLE, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(PROFILE_COMPLETE_TEXT, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Browse courses", onBrowseCourses)
        }
    }
}

@Composable
private fun Field(value: String, label: String, keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = singleLine, keyboardOptions = KeyboardOptions(keyboardType = keyboard))
}

// =============================================================== payments

@Composable
fun PaymentsScreen(onBack: () -> Unit) {
    val vm: LoaderViewModel<Pair<PaymentHistory, List<Enrollment>>> = viewModel(key = "payments", factory = vmFactory { c ->
        LoaderViewModel<Pair<PaymentHistory, List<Enrollment>>> {
            when (val h = c.repo.paymentHistory()) {
                is ApiResult.Ok -> ApiResult.Ok(h.value to (c.repo.enrollments().orNull() ?: emptyList()))
                is ApiResult.Err -> h
            }
        }
    })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar("Payments", onBack) }) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(state, vm::load) { (history, enrollments) ->
                val titles = enrollments.associate { it.courseId to (it.course?.title ?: "") }
                if (history.submissions.isEmpty() && history.records.isEmpty()) EmptyState("No payments yet", "Fees you pay in the app or on the website appear here.")
                else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // As on the website: receipts are not shown here; the academy e-mails them once issued.
                    item { Banner(RECEIPT_BY_EMAIL, Tone.Info) }
                    val (awaiting, settled) = history.submissions.sortedByDescending { it.createdAt }
                        .partition { it.status.equals("pending_verification", ignoreCase = true) }
                    if (awaiting.isNotEmpty()) item { Spacer(Modifier.height(4.dp)); Overline("Awaiting Approval") }
                    if (awaiting.isNotEmpty()) items(awaiting, key = { it.id }) { s -> SubmissionCard(s, titles) }
                    if (settled.isNotEmpty()) item { Spacer(Modifier.height(4.dp)); Overline("Payment History") }
                    items(settled, key = { it.id }) { s -> SubmissionCard(s, titles) }
                    val recordsWithoutSubmission = history.records.filter { r -> history.submissions.none { it.paymentRecordId == r.id } }
                    if (recordsWithoutSubmission.isNotEmpty()) item { Spacer(Modifier.height(8.dp)); Overline("Other payments recorded by the academy") }
                    items(recordsWithoutSubmission, key = { "r" + it.id }) { r ->
                        AppCard(Modifier.fillMaxWidth()) {
                            Row {
                                Column(Modifier.weight(1f)) {
                                    Text(r.description ?: titleCase(r.paymentType ?: "Payment"), style = MaterialTheme.typography.titleMedium)
                                    Text(formatDate(r.paidAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(rupees(r.amountInrPaise), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val RECEIPT_BY_EMAIL =
    "The academy e-mails your receipt once your payment is verified and the receipt is issued."

@Composable
private fun SubmissionCard(s: PaymentSubmission, titles: Map<String, String>) {
                        val (label, tone) = statusLabel(s.status, StatusKind.Payment)
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
                        }
}

// ========================================================== notifications

@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpenHref: (String) -> Unit) {
    val vm: PagedViewModel<Notification> = viewModel(key = "notifications", factory = vmFactory { c -> PagedViewModel { p, _, f -> c.repo.notifications(p, f == "unread") } })
    val container = rememberContainer()
    val state by vm.state.collectAsStateWithLifecycle()
    // App scope: opening a notification leaves this screen, which must not cancel marking it read.
    Scaffold(topBar = { BackTopBar("Notifications", onBack, actions = { TextButton({ container.appScope.launch { container.repo.markAllRead(); vm.refresh() } }) { Text("Mark all read") } }) }) { padding ->
        PagedListBody(vm, null, "You're all caught up", { it.id }, Modifier.padding(padding), header = {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilterChip(state.filter == null, { vm.filter(null) }, { Text("All") })
                    androidx.compose.material3.FilterChip(state.filter == "unread", { vm.filter("unread") }, { Text("Unread") })
                }
            }
        }) { n ->
            val unread = n.readAt == null
            AppCard(Modifier.fillMaxWidth(), onClick = { container.appScope.launch { container.repo.notification(n.id) }; onOpenHref(n.href) },
                color = if (unread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLowest) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unread) { Box(Modifier.size(8.dp).clip(CircleShape).background(Brand.goldSoft)); Spacer(Modifier.width(8.dp)) }
                    Text(n.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(relativeTime(n.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                n.body?.let { Spacer(Modifier.height(4.dp)); Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                relatedPageLabel(n.href)?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ======================================================== change password

@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = rememberContainer().auth
    val submitter = rememberSubmitter("change-password")
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    LaunchedEffect(submitter.result) { if (submitter.result?.ok == true) { current = ""; new = "" } }
    Scaffold(topBar = { BackTopBar("Change password", onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            submitter.result?.let { Banner(it.message, if (it.ok) Tone.Success else Tone.Danger) }
            PasswordField(current, "Current password", {}) { current = it }
            PasswordField(new, "New password (at least 8 characters)", {}) { new = it }
            Text("Other phones signed in to your account will be signed out.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton("Change password", {
                val (from, to) = current to new
                submitter.submit {
                    when (val r = auth.changePassword(from, to)) {
                        is ApiResult.Ok -> SubmitResult("Password changed.", ok = true)
                        is ApiResult.Err -> SubmitResult(r.message, ok = false)
                    }
                }
            }, enabled = current.isNotEmpty() && new.length >= 8, loading = submitter.busy)
        }
    }
}

// ========================================================= delete account

@Composable
fun DeleteAccountScreen(onBack: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val container = rememberContainer()
    val submitter = rememberSubmitter("delete-account")
    // Whether the account has a password decides what confirms the deletion; guessing "yes" when the profile didn't
    // load left a Google-only user with a password field they couldn't fill.
    var profileAttempt by remember { mutableIntStateOf(0) }
    var hasPassword by remember { mutableStateOf<Load<Boolean>>(Load.Loading) }
    LaunchedEffect(profileAttempt) { hasPassword = Load.Loading; hasPassword = container.auth.profile().map { it.hasPassword }.toLoad() }
    var password by remember { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val busy = submitter.busy
    val error = submitter.result?.takeIf { !it.ok }?.message
    val done = submitter.result?.ok == true
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
            when (val h = hasPassword) {
                Load.Loading -> LoadingBox(Modifier.fillMaxWidth().height(60.dp))
                is Load.Failed -> ErrorState(h.message, { profileAttempt++ })
                is Load.Ready -> if (h.value) PasswordField(password, "Your password", {}) { password = it }
                else OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), label = { Text("Type DELETE to confirm") }, singleLine = true)
            }
            androidx.compose.material3.Button(
                onClick = {
                    val (pass, typed) = password.ifBlank { null } to confirm.ifBlank { null }
                    submitter.submit {
                        when (val r = container.auth.deleteAccount(pass, typed)) {
                            is ApiResult.Ok -> { container.cart.clearLocal(); SubmitResult("Account deleted.", ok = true) }
                            is ApiResult.Err -> SubmitResult(r.message, ok = false)
                        }
                    }
                },
                enabled = !busy && when (val h = hasPassword) {
                    is Load.Ready -> if (h.value) password.isNotEmpty() else confirm == "DELETE"
                    else -> false
                },
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
    val container = rememberContainer()
    val session by container.sessionStore.session.collectAsStateWithLifecycle()
    var config by remember { mutableStateOf<AppConfig?>(null) }
    LaunchedEffect(Unit) { config = container.repo.appConfig().orNull() }
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
            ContactForm(session?.user)
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
                com.darsequran.academy.ui.common.SecondaryButton("Visit the website", { openWeb(context, "/about") })
            }
        }
    }
}

private fun roleLabel(role: String?): String? = when (role) {
    Roles.ADMIN -> "Administrator"
    Roles.DEVELOPER -> "Developer"
    Roles.TEACHER -> "Teacher"
    else -> null
}
