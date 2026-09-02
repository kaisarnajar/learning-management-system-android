package com.darsequran.academy.ui.main

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darsequran.academy.ui.theme.EmeraldDark
import com.darsequran.academy.ui.theme.EmeraldPrimary
import com.darsequran.academy.ui.theme.GoldAccent

@Composable
fun MoreMenuSheet(
    onSelectCourses: () -> Unit,
    onSelectLibrary: () -> Unit,
    onSelectAnnouncements: () -> Unit,
    onSelectBlog: () -> Unit,
    onSelectTeachers: () -> Unit,
    onSelectFatwa: () -> Unit,
    onSelectBookstore: () -> Unit,
    onSelectReviews: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Academy Explorer Hub",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            )
            Text(
                text = "Access courses, library, blog & student services",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Courses Directory
            MoreMenuItem(
                icon = Icons.Default.School,
                title = "Public Courses Directory",
                subtitle = "Browse all offered Tajweed, Arabic & Quranic courses",
                onClick = onSelectCourses
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Digital Library
            MoreMenuItem(
                icon = Icons.Default.Description,
                title = "Digital Library & Resources",
                subtitle = "Read & download authentic Islamic PDFs & eBooks",
                onClick = onSelectLibrary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Announcements
            MoreMenuItem(
                icon = Icons.Default.Campaign,
                title = "Academy Announcements",
                subtitle = "Notice board, batch alerts & exam schedules",
                onClick = onSelectAnnouncements
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Blog
            MoreMenuItem(
                icon = Icons.AutoMirrored.Filled.Article,
                title = "Academy Blog & Articles",
                subtitle = "Tajweed tips, Quranic reflections & guides",
                onClick = onSelectBlog
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Teachers Directory
            MoreMenuItem(
                icon = Icons.Default.People,
                title = "Faculty & Scholars",
                subtitle = "Meet our qualified instructors & scholars",
                onClick = onSelectTeachers
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Fatwa
            MoreMenuItem(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "Fatwa & Fiqh Repository",
                subtitle = "Search Islamic rulings & ask questions to scholars",
                onClick = onSelectFatwa
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Bookstore
            MoreMenuItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Bookstore & Quran Editions",
                subtitle = "Order printed Quran editions, Tajweed books & literature",
                onClick = onSelectBookstore
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 8. Reviews
            MoreMenuItem(
                icon = Icons.Default.RateReview,
                title = "My Reviews & Feedback",
                subtitle = "Share your learning experience with the academy",
                onClick = onSelectReviews
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Item Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Sign Out / Logout",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(EmeraldDark.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = GoldAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}
