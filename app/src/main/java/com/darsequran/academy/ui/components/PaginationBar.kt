package com.darsequran.academy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalCount: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1 && totalCount <= 0) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val countText = if (totalCount == 1) "1 item" else "$totalCount items"
        Text(
            text = if (totalPages > 1) "Page $currentPage of $totalPages · $countText" else "Showing $countText",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (totalPages > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (currentPage > 1) onPageSelected(currentPage - 1) },
                    enabled = currentPage > 1,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Previous",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val pageNumbers = getPageNumbersList(currentPage, totalPages)
                pageNumbers.forEachIndexed { index, pageNum ->
                    val prevNum = pageNumbers.getOrNull(index - 1)
                    if (prevNum != null && pageNum - prevNum > 1) {
                        Text(
                            text = "…",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }

                    val isSelected = pageNum == currentPage
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(36.dp)
                            .clickable { if (!isSelected) onPageSelected(pageNum) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        tonalElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = pageNum.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { if (currentPage < totalPages) onPageSelected(currentPage + 1) },
                    enabled = currentPage < totalPages,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

private fun getPageNumbersList(currentPage: Int, totalPages: Int): List<Int> {
    if (totalPages <= 5) {
        return (1..totalPages).toList()
    }
    val pages = mutableSetOf(1, totalPages, currentPage)
    if (currentPage > 1) pages.add(currentPage - 1)
    if (currentPage < totalPages) pages.add(currentPage + 1)
    return pages.sorted()
}
