package com.darsequran.academy.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.darsequran.academy.R
import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.data.model.UserDto
import com.darsequran.academy.data.remote.RetrofitClient
import com.darsequran.academy.ui.theme.EmeraldDark
import com.darsequran.academy.ui.theme.EmeraldPrimary
import com.darsequran.academy.ui.theme.GoldAccent
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class OccupationChoice(val value: String, val label: String)

val OCCUPATION_OPTIONS = listOf(
    OccupationChoice("STUDENT", "Student"),
    OccupationChoice("WORKING", "Working (private sector)"),
    OccupationChoice("GOVERNMENT_EMPLOYEE", "Government employee"),
    OccupationChoice("SELF_EMPLOYED", "Self-employed / business owner"),
    OccupationChoice("LABOURER", "Labour / daily wage worker"),
    OccupationChoice("POLICE_OFFICER", "Police officer"),
    OccupationChoice("ARMED_FORCES", "Armed forces"),
    OccupationChoice("TEACHER", "Teacher / educator"),
    OccupationChoice("HEALTHCARE_WORKER", "Healthcare worker"),
    OccupationChoice("ENGINEER", "Engineer"),
    OccupationChoice("IT_PROFESSIONAL", "IT / software professional"),
    OccupationChoice("ACCOUNTANT", "Accountant / finance"),
    OccupationChoice("LAWYER", "Lawyer / legal professional"),
    OccupationChoice("DRIVER", "Driver / transport worker"),
    OccupationChoice("FARMER", "Farmer / agriculture"),
    OccupationChoice("SHOPKEEPER", "Shopkeeper / retail"),
    OccupationChoice("CLERGY", "Imam / religious scholar"),
    OccupationChoice("HOMEMAKER", "Homemaker"),
    OccupationChoice("RETIRED", "Retired"),
    OccupationChoice("UNEMPLOYED", "Unemployed")
)

fun formatDateOfBirthDisplay(dobString: String?): String {
    if (dobString.isNullOrBlank()) return "Not provided"
    return try {
        val cleanStr = dobString.take(10) // e.g. "1999-01-03"
        val parts = cleanStr.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val monthIndex = parts[1].toIntOrNull() ?: 1
            val day = parts[2].toIntOrNull() ?: 1
            val monthNames = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            "$day ${monthNames.getOrElse(monthIndex - 1) { "January" }} $year"
        } else dobString
    } catch (_: Exception) {
        dobString
    }
}

fun formatOccupationDisplay(occupation: String?): String {
    if (occupation.isNullOrBlank()) return "Not provided"
    val match = OCCUPATION_OPTIONS.find { it.value.equals(occupation, ignoreCase = true) }
    return match?.label ?: occupation.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

fun formatGenderDisplay(gender: String?): String {
    if (gender.isNullOrBlank()) return "Not provided"
    return when (gender.uppercase()) {
        "MALE" -> "Male"
        "FEMALE" -> "Female"
        else -> gender
    }
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    tokenManager: TokenManager,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            val user = uiState.user

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Profile Header Card matching Web UI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            UserProfileAvatar(
                                imageUrl = user?.image,
                                gender = user?.gender,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Column {
                            Text(
                                text = user?.name ?: "Kaisar Ahmad Najar",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = EmeraldPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Student ID: ${user?.registrationNumber ?: "DQA2026-00001"}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Personal Information Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Personal Info",
                            tint = GoldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 18.sp
                            )
                        )
                    }

                    TextButton(onClick = { viewModel.openEditDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Details",
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit Details",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Personal Information Grid Items matching Web Design
                ProfileInfoCard(
                    icon = Icons.Default.Email,
                    label = "Email Address",
                    value = user?.email ?: "kaisarnajar11114@gmail.com"
                )

                Spacer(modifier = Modifier.height(10.dp))

                ProfileInfoCard(
                    icon = Icons.Default.Phone,
                    label = "Phone Number / WhatsApp",
                    value = user?.whatsapp ?: "+91 7006025120"
                )

                Spacer(modifier = Modifier.height(10.dp))

                ProfileInfoCard(
                    icon = Icons.Default.FamilyRestroom,
                    label = "Father's Name",
                    value = user?.fatherName ?: "Mohammad Akbar Najar"
                )

                Spacer(modifier = Modifier.height(10.dp))

                ProfileInfoCard(
                    icon = Icons.Default.CalendarToday,
                    label = "Date of Birth",
                    value = formatDateOfBirthDisplay(user?.dateOfBirth)
                )

                Spacer(modifier = Modifier.height(10.dp))

                ProfileInfoCard(
                    icon = Icons.Default.Person,
                    label = "Gender",
                    value = formatGenderDisplay(user?.gender)
                )

                Spacer(modifier = Modifier.height(10.dp))

                ProfileInfoCard(
                    icon = Icons.Default.BusinessCenter,
                    label = "Occupation",
                    value = formatOccupationDisplay(user?.occupation)
                )

                Spacer(modifier = Modifier.height(10.dp))

                ProfileInfoCard(
                    icon = Icons.Default.LocationOn,
                    label = "Address",
                    value = user?.address ?: "Kampora Tangmarg"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Sign Out Section Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSignOutDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sign Out",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Log out from your Darse Quran Academy account",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            shape = RoundedCornerShape(18.dp),
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Sign Out Icon",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Sign Out",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out from your Darse Quran Academy account?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        scope.launch {
                            tokenManager.clearSession()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SIGN OUT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = EmeraldPrimary, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    // Edit Profile Modal Dialog
    if (uiState.showEditDialog) {
        EditProfileDialog(
            user = uiState.user,
            isUpdating = uiState.isUpdating,
            onDismiss = { viewModel.closeEditDialog() },
            onSave = { name, fatherName, dob, occupation, address, phone, gender, image ->
                viewModel.updateProfile(name, fatherName, dob, occupation, address, phone, gender, image)
            }
        )
    }
}

@Composable
fun ProfileInfoCard(
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(EmeraldDark.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    user: UserDto?,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, fatherName: String, dob: String, occupation: String, address: String, phone: String, gender: String, image: String?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(user?.name ?: "") }
    var fatherName by remember { mutableStateOf(user?.fatherName ?: "") }
    var dateOfBirth by remember { mutableStateOf(user?.dateOfBirth?.take(10) ?: "") }
    var selectedOccupationValue by remember { mutableStateOf(user?.occupation ?: "IT_PROFESSIONAL") }
    var address by remember { mutableStateOf(user?.address ?: "") }
    var phone by remember { mutableStateOf(user?.whatsapp ?: "") }
    var genderValue by remember { mutableStateOf(user?.gender?.uppercase() ?: "MALE") }
    var imageBase64 by remember { mutableStateOf<String?>(user?.image) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var occupationExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedBitmap = bitmap

                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                imageBase64 = "data:image/jpeg;base64,$encoded"
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EmeraldPrimary.copy(alpha = 0.04f))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Edit Personal Details",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "Update your profile details & contact info",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Photo Upload Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldDark)
                                    .border(2.dp, GoldAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedBitmap != null) {
                                    Image(
                                        bitmap = selectedBitmap!!.asImageBitmap(),
                                        contentDescription = "New Profile Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    UserProfileAvatar(
                                        imageUrl = imageBase64 ?: user?.image,
                                        gender = genderValue,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Profile Photo",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (genderValue == "FEMALE") {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "Photo upload disabled for female students.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = { photoPickerLauncher.launch("image/*") },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, EmeraldPrimary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Upload Photo",
                                                modifier = Modifier.size(15.dp),
                                                tint = EmeraldPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (selectedBitmap != null || (imageBase64 != null && imageBase64 != user?.image)) "Change Photo" else "Upload Photo",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EmeraldPrimary
                                            )
                                        }

                                        if (selectedBitmap != null || (imageBase64 != null && imageBase64 != user?.image)) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    selectedBitmap = null
                                                    imageBase64 = user?.image
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove photo",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                    val fieldShape = RoundedCornerShape(12.dp)

                    // Full Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Father's Name
                    OutlinedTextField(
                        value = fatherName,
                        onValueChange = { fatherName = it },
                        label = { Text("Father's Name") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.FamilyRestroom, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date of Birth
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { dateOfBirth = it },
                        label = { Text("Date of Birth (YYYY-MM-DD)") },
                        placeholder = { Text("1999-01-03") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone / WhatsApp
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number / WhatsApp") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gender Selection Dropdown
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded }
                    ) {
                        OutlinedTextField(
                            value = if (genderValue == "FEMALE") "Female" else "Male",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Wc, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Male", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    genderValue = "MALE"
                                    genderExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Female", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    genderValue = "FEMALE"
                                    genderExpanded = false
                                    selectedBitmap = null
                                    imageBase64 = null
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Occupation Selection Dropdown
                    val currentOccupationLabel = formatOccupationDisplay(selectedOccupationValue)

                    ExposedDropdownMenuBox(
                        expanded = occupationExpanded,
                        onExpandedChange = { occupationExpanded = !occupationExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentOccupationLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Occupation") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.BusinessCenter, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = occupationExpanded) },
                            shape = fieldShape,
                            colors = fieldColors,
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = occupationExpanded,
                            onDismissRequest = { occupationExpanded = false }
                        ) {
                            OCCUPATION_OPTIONS.forEach { choice ->
                                DropdownMenuItem(
                                    text = { Text(choice.label) },
                                    onClick = {
                                        selectedOccupationValue = choice.value
                                        occupationExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Address Field
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        },
                        minLines = 2,
                        maxLines = 3,
                        singleLine = false,
                        shape = fieldShape,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Action Buttons Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            onSave(
                                name,
                                fatherName,
                                dateOfBirth,
                                selectedOccupationValue,
                                address,
                                phone,
                                genderValue,
                                if (genderValue == "FEMALE") null else imageBase64
                            )
                        },
                        enabled = !isUpdating,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = "Save Changes",
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileAvatar(
    imageUrl: String?,
    gender: String?,
    modifier: Modifier = Modifier
) {
    val defaultIconRes = if (gender?.uppercase() == "FEMALE") R.drawable.female_icon else R.drawable.male_icon

    val cleanedUrl = remember(imageUrl) {
        when {
            imageUrl.isNullOrBlank() -> null
            imageUrl == "/assets/female_icon.png" -> "ASSET_FEMALE"
            imageUrl == "/assets/male_icon.png" -> "ASSET_MALE"
            imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> imageUrl
            imageUrl.startsWith("data:image/") -> imageUrl
            imageUrl.startsWith("/") -> {
                val base = RetrofitClient.PRODUCTION_BASE_URL
                    .removeSuffix("api/v1/")
                    .removeSuffix("/")
                "$base$imageUrl"
            }
            else -> imageUrl
        }
    }

    val base64Bitmap = remember(cleanedUrl) {
        if (cleanedUrl != null && cleanedUrl.startsWith("data:image/")) {
            try {
                val pureBase64 = cleanedUrl.substringAfter(",")
                val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    when {
        base64Bitmap != null -> {
            Image(
                bitmap = base64Bitmap.asImageBitmap(),
                contentDescription = "Profile Avatar",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        cleanedUrl == "ASSET_FEMALE" -> {
            Image(
                painter = painterResource(id = R.drawable.female_icon),
                contentDescription = "Female Avatar",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        cleanedUrl == "ASSET_MALE" -> {
            Image(
                painter = painterResource(id = R.drawable.male_icon),
                contentDescription = "Male Avatar",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
        !cleanedUrl.isNullOrBlank() && (cleanedUrl.startsWith("http://") || cleanedUrl.startsWith("https://")) -> {
            AsyncImage(
                model = cleanedUrl,
                contentDescription = "Profile Photo",
                modifier = modifier,
                contentScale = ContentScale.Crop,
                error = painterResource(id = defaultIconRes),
                placeholder = painterResource(id = defaultIconRes)
            )
        }
        else -> {
            Image(
                painter = painterResource(id = defaultIconRes),
                contentDescription = "Profile Avatar",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
    }
}
