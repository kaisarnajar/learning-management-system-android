package com.darsequran.academy.ui.auth

import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import com.darsequran.academy.ui.theme.HeroUnderStatusBar
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darsequran.academy.BuildConfig
import com.darsequran.academy.R
import com.darsequran.academy.data.ApiResult
import com.darsequran.academy.data.remote.Profile
import com.darsequran.academy.data.repo.AuthRepository
import com.darsequran.academy.ui.common.Banner
import com.darsequran.academy.ui.common.BackTopBar
import com.darsequran.academy.ui.common.HeroBox
import com.darsequran.academy.ui.common.MessageState
import com.darsequran.academy.ui.common.PrimaryButton
import com.darsequran.academy.ui.common.SecondaryButton
import com.darsequran.academy.ui.common.Tone
import com.darsequran.academy.ui.common.openWeb
import com.darsequran.academy.ui.common.rememberContainer
import com.darsequran.academy.ui.common.vmFactory
import com.darsequran.academy.ui.theme.Brand
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** [googleSignedIn]: done by Google sign-in, which needs no e-mail verification (unlike [AuthViewModel.register]). */
data class AuthState(
    val busy: Boolean = false,
    val error: String? = null,
    val unverifiedEmail: String? = null,
    val info: String? = null,
    val done: Boolean = false,
    val googleSignedIn: Boolean = false,
)

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** One request at a time: a second tap while one is on its way (even in the same frame) is ignored. */
    private fun run(block: suspend () -> ApiResult<*>, onOk: () -> Unit = { _state.value = AuthState(done = true) }) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, error = null, info = null)
        viewModelScope.launch {
            when (val r = block()) {
                is ApiResult.Ok -> onOk()
                is ApiResult.Err -> _state.value = _state.value.copy(busy = false, error = r.message, unverifiedEmail = null)
            }
        }
    }

    fun login(email: String, password: String) {
        if (_state.value.busy) return
        _state.value = AuthState(busy = true)
        viewModelScope.launch {
            when (val r = auth.login(email, password)) {
                is ApiResult.Ok -> _state.value = AuthState(done = true)
                is ApiResult.Err -> _state.value = AuthState(error = r.message, unverifiedEmail = if (r.code == "email_not_verified") email.trim() else null)
            }
        }
    }

    fun register(name: String, email: String, password: String) = run({ auth.register(name, email, password) })

    /** Arms [GoogleWelcome] first: saving the session replaces the navigation graph, this screen included. */
    fun google(idToken: String) {
        if (_state.value.busy) return
        GoogleWelcome.arm()
        run({ auth.google(idToken).also { if (it is ApiResult.Err) GoogleWelcome.disarm() } }) {
            _state.value = AuthState(done = true, googleSignedIn = true)
        }
    }

    fun googleFailed(message: String) { _state.value = AuthState(error = message) }

    /** The website's register page refuses Google sign-in until the policies are accepted. */
    fun policiesNotAccepted() { _state.value = AuthState(error = POLICIES_REQUIRED) }

    fun resend(email: String) = run({ auth.resendVerification(email) }) {
        _state.value = AuthState(info = "If that account still needs verifying, a new link is on its way to $email.")
    }

    fun forgot(email: String) = run({ auth.forgotPassword(email) }) {
        _state.value = AuthState(done = true)
    }
}

/** Google sign-in through Credential Manager; returns the ID token, or the failure. Leaving the screen cancels it. */
suspend fun requestGoogleIdToken(context: Context): Result<String> = try {
    val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .build()
    val result = CredentialManager.create(context).getCredential(context, GetCredentialRequest(listOf(option)))
    val credential = result.credential
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
    } else Result.failure(IllegalStateException("Unexpected credential"))
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

val googleSignInAvailable: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

// ================================================================ sign in

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    vm: AuthViewModel = viewModel(key = "signin", factory = vmFactory { AuthViewModel(it.auth) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // not saveable: saved state can be written to disk

    AuthFrame("Welcome back", SIGN_IN_SUBTITLE, onBack) {
        state.error?.let { Banner(it, Tone.Danger) }
        state.info?.let { Banner(it, Tone.Success) }
        state.unverifiedEmail?.let { addr -> TextButton({ vm.resend(addr) }) { Text("Send the verification e-mail again") } }
        EmailField(email) { email = it }
        // The keyboard's Done follows the same rule as the button.
        val canSignIn = email.contains("@") && password.isNotEmpty()
        PasswordField(password, "Password", onDone = { if (canSignIn) vm.login(email, password) }) { password = it }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { TextButton(onForgot) { Text("Forgot password?") } }
        PrimaryButton("Sign in", { vm.login(email, password) }, enabled = canSignIn, loading = state.busy)
        GoogleButton(vm)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("New to the academy?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onRegister) { Text("Create an account") }
        }
    }
}

/** [canStart] is asked first; false stops before Google's account picker opens (it reports why itself). */
@Suppress("FunctionNaming") // a composable, named like one
@Composable
private fun GoogleButton(vm: AuthViewModel, canStart: () -> Boolean = { true }) {
    if (!googleSignInAvailable) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f)); Text("  or  ", style = MaterialTheme.typography.bodySmall); HorizontalDivider(Modifier.weight(1f))
    }
    SecondaryButton("Continue with Google", {
        if (canStart()) scope.launch {
            requestGoogleIdToken(context).fold(
                onSuccess = vm::google,
                onFailure = {
                    when (it) {
                        is GetCredentialCancellationException -> Unit
                        is NoCredentialException -> vm.googleFailed("There's no Google account on this phone. Add one in Settings, or use your e-mail.")
                        else -> vm.googleFailed("Google sign-in didn't work on this phone. Use your e-mail instead.")
                    }
                },
            )
        }
    })
}

// =============================================================== register

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    privacyUrl: String,
    termsUrl: String,
    vm: AuthViewModel = viewModel(key = "register", factory = vmFactory { AuthViewModel(it.auth) }),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    // Google accounts are already verified: go straight on, without the "check your e-mail" step.
    LaunchedEffect(state.googleSignedIn) { if (state.googleSignedIn) onDone() }

    if (state.done && !state.googleSignedIn) {
        Scaffold { p ->
            Box(Modifier.padding(p).fillMaxSize(), contentAlignment = Alignment.Center) {
                MessageState(
                    Icons.Rounded.MarkEmailRead,
                    "Check your e-mail",
                    "We sent a verification link to $email. Open it to activate your account, then sign in.",
                    "Continue",
                    onDone,
                )
            }
        }
        return
    }

    AuthFrame("Create Account", REGISTER_SUBTITLE, onBack) {
        state.error?.let { Banner(it, Tone.Danger) }
        OutlinedTextField(name, { name = it.take(100) }, Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true)
        EmailField(email) { email = it }
        PasswordField(password, "Password (at least 8 characters)", onDone = {}) { password = it }
        // The whole row ticks the box; only the two links open the documents.
        val linkStyle = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold))
        Row(
            Modifier.fillMaxWidth().toggleable(accepted, role = Role.Checkbox) { accepted = it },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(accepted, null, Modifier.padding(end = 12.dp))
            Text(
                buildAnnotatedString {
                    append("I have read and agree to the ")
                    withLink(LinkAnnotation.Clickable("terms", linkStyle) { openWeb(context, termsUrl) }) {
                        append("Terms & Conditions")
                    }
                    append(" and ")
                    withLink(LinkAnnotation.Clickable("privacy", linkStyle) { openWeb(context, privacyUrl) }) {
                        append("Privacy Policy")
                    }
                    append(".")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        PrimaryButton(
            "Create account",
            { vm.register(name, email, password) },
            enabled = accepted && email.contains("@") && password.length >= 8 && name.trim().length >= 2,
            loading = state.busy,
        )
        GoogleButton(vm, canStart = { accepted.also { ok -> if (!ok) vm.policiesNotAccepted() } })
    }
}

// ================================================================= forgot

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, vm: AuthViewModel = viewModel(key = "forgot", factory = vmFactory { AuthViewModel(it.auth) })) {
    val state by vm.state.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { BackTopBar("Reset password", onBack) }) { p ->
        Column(Modifier.padding(p).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.done) {
                MessageState(Icons.Rounded.MarkEmailRead, "Check your e-mail", "If an account uses $email, a reset link is on its way. Open it on this phone to choose a new password.", "Back to sign in", onBack)
            } else {
                Text("Enter your account's e-mail. We'll send you a link to choose a new password.", style = MaterialTheme.typography.bodyLarge)
                state.error?.let { Banner(it, Tone.Danger) }
                EmailField(email) { email = it }
                PrimaryButton("Send reset link", { vm.forgot(email) }, enabled = email.contains("@"), loading = state.busy)
            }
        }
    }
}

// ================================================================ pieces

@Composable
private fun AuthFrame(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).imePadding()) {
        HeroUnderStatusBar()
        HeroBox(Modifier.fillMaxWidth()) {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(bottom = 28.dp)) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Brand.onHero) }
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.logo), null, Modifier.size(72.dp).clip(CircleShape).background(Color.White))
                    Spacer(Modifier.height(14.dp))
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = Brand.onHero)
                    Spacer(Modifier.height(6.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Brand.onHero.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                }
            }
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
}

@Composable
fun EmailField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, { onChange(it.trim()) }, Modifier.fillMaxWidth(),
        label = { Text("E-mail") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    )
}

@Composable
fun PasswordField(value: String, label: String, onDone: () -> Unit, onChange: (String) -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value, { onChange(it.take(128)) }, Modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton({ visible = !visible }) { Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (visible) "Hide password" else "Show password") } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

// ======================================================== website wording

/** The website's sign-in subtitle (components/auth/LoginForm.tsx). */
internal const val SIGN_IN_SUBTITLE = "Students and instructors use the same sign-in. After the academy links your " +
    "registered email as a teacher, you will open the teacher portal when you sign in."

/** The website's register subtitle (components/auth/RegisterForm.tsx). */
internal const val REGISTER_SUBTITLE = "Register to enroll in courses. If the academy adds you as a teacher later, " +
    "sign in with this same email to access the teacher portal."

/** The website's error for Google sign-up before the policies are accepted. */
internal const val POLICIES_REQUIRED = "Please agree to the Terms & Conditions and Privacy Policy to continue."

/** The website's banner on /profile?source=google (app/profile/page.tsx). */
internal const val GOOGLE_WELCOME_TITLE = "Welcome! You signed in with Google."
internal const val GOOGLE_WELCOME_BODY = "Your email is verified automatically. Please fill in the required profile " +
    "details below — the academy needs them before you can enroll in any course."

// ======================================================= google welcome

/**
 * The website's /auth/continue step: a student who signs in with Google and whose profile is incomplete is sent to
 * the profile form with a welcome banner. Signing in replaces the app's navigation graph (it is keyed by the user),
 * so the sign-in screen can't do the sending; it arms this before signing in, and the new graph consumes it.
 */
object GoogleWelcome {
    private val pending = AtomicBoolean(false)

    fun arm() = pending.set(true)

    fun disarm() = pending.set(false)

    /** True, once per Google sign-in, when [profile] says a student still has to complete their profile. */
    suspend fun consume(profile: suspend () -> ApiResult<Profile>): Boolean =
        pending.getAndSet(false) && needsProfileAfterGoogle(profile())
}

/** As /auth/continue: only students (role USER) are held to a complete profile; an unknown profile sends no one. */
internal fun needsProfileAfterGoogle(profile: ApiResult<Profile>): Boolean =
    profile is ApiResult.Ok && profile.value.role == "USER" && !profile.value.profileComplete

/** Opens the profile form ([onIncompleteProfile]) after a Google sign-in that needs it, with the welcome message. */
@Suppress("FunctionNaming") // a composable, named like one
@Composable
fun GoogleWelcomeEffect(onIncompleteProfile: () -> Unit) {
    val container = rememberContainer()
    var welcome by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (GoogleWelcome.consume { container.auth.profile() }) { onIncompleteProfile(); welcome = true }
    }
    if (welcome) AlertDialog(
        onDismissRequest = { welcome = false },
        confirmButton = { TextButton({ welcome = false }) { Text("OK") } },
        title = { Text(GOOGLE_WELCOME_TITLE) },
        text = { Text(GOOGLE_WELCOME_BODY) },
    )
}
