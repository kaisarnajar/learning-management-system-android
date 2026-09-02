package com.darsequran.academy.ui.profile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darsequran.academy.R
import com.darsequran.academy.data.model.UserDto
import com.darsequran.academy.ui.theme.EmeraldDark
import com.darsequran.academy.ui.theme.EmeraldPrimary
import com.darsequran.academy.ui.theme.GoldAccent

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
    return when (occupation.uppercase()) {
        "IT_PROFESSIONAL" -> "IT / software professional"
        "STUDENT" -> "Student"
        "WORKING" -> "Working (private sector)"
        "GOVERNMENT_EMPLOYEE" -> "Government employee"
        "SELF_EMPLOYED" -> "Self-employed / business owner"
        "LABOURER" -> "Labour / daily wage worker"
        "POLICE_OFFICER" -> "Police officer"
        "ARMED_FORCES" -> "Armed forces"
        "TEACHER" -> "Teacher / educator"
        "HEALTHCARE_WORKER" -> "Healthcare worker"
        "ENGINEER" -> "Engineer"
        "ACCOUNTANT" -> "Accountant / finance"
        "LAWYER" -> "Lawyer / legal professional"
        "DRIVER" -> "Driver / transport worker"
        "FARMER" -> "Farmer / agriculture"
        "SHOPKEEPER" -> "Shopkeeper / retail"
        "CLERGY" -> "Imam / religious scholar"
        "HOMEMAKER" -> "Homemaker"
        "RETIRED" -> "Retired"
        "UNEMPLOYED" -> "Unemployed"
        else -> occupation.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
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
    viewModel: ProfileViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Edit Profile Modal Dialog
    if (uiState.showEditDialog) {
        EditProfileDialog(
            user = uiState.user,
            isUpdating = uiState.isUpdating,
            onDismiss = { viewModel.closeEditDialog() },
            onSave = { name, fatherName, dob, occupation, address, phone, gender ->
                viewModel.updateProfile(name, fatherName, dob, occupation, address, phone, gender)
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

@Composable
fun EditProfileDialog(
    user: UserDto?,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, fatherName: String, dob: String, occupation: String, address: String, phone: String, gender: String) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var fatherName by remember { mutableStateOf(user?.fatherName ?: "") }
    var dateOfBirth by remember { mutableStateOf(user?.dateOfBirth ?: "") }
    var occupation by remember { mutableStateOf(user?.occupation ?: "") }
    var address by remember { mutableStateOf(user?.address ?: "") }
    var phone by remember { mutableStateOf(user?.whatsapp ?: "") }
    var gender by remember { mutableStateOf(user?.gender ?: "Male") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father's Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number / WhatsApp") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it },
                    label = { Text("Gender (Male / Female)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    label = { Text("Occupation") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                onClick = { onSave(name, fatherName, dateOfBirth, occupation, address, phone, gender) },
                enabled = !isUpdating,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("SAVE CHANGES")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = EmeraldPrimary)
            }
        }
    )
}
