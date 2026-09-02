package com.darsequran.academy.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.darsequran.academy.R
import com.darsequran.academy.data.local.TokenManager
import com.darsequran.academy.data.model.UserDto
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
            val avatarDrawable = if (user?.gender?.uppercase() == "FEMALE") {
                R.drawable.female_icon
            } else {
                R.drawable.male_icon
            }

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
                            Image(
                                painter = painterResource(id = avatarDrawable),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
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

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Edit Personal Details",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Photo Section matching Web Logic
                Text(
                    text = "Profile Photo ${if (genderValue == "FEMALE") "(Disabled for Female students)" else "(Optional)"}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(EmeraldDark)
                            .border(1.dp, GoldAccent, CircleShape),
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
                            val iconRes = if (genderValue == "FEMALE") R.drawable.female_icon else R.drawable.male_icon
                            Image(
                                painter = painterResource(id = iconRes),
                                contentDescription = "Profile Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    if (genderValue == "FEMALE") {
                        Text(
                            text = "Photo upload is disabled for Female students.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        )
                    } else {
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Upload Photo",
                                modifier = Modifier.size(16.dp),
                                tint = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photo", fontSize = 12.sp, color = EmeraldPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father's Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    placeholder = { Text("1999-01-03") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "DOB", tint = EmeraldPrimary)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number / WhatsApp") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Male") },
                            onClick = {
                                genderValue = "MALE"
                                genderExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Female") },
                            onClick = {
                                genderValue = "FEMALE"
                                genderExpanded = false
                                selectedBitmap = null
                                imageBase64 = null
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = occupationExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
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

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
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
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = EmeraldPrimary, fontWeight = FontWeight.Medium)
            }
        }
    )
}
