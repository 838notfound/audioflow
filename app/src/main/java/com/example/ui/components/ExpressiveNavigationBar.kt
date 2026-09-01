package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.MainDestination
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ExpressiveNavigationBar(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val queueBadge by viewModel.queueBadgeCount.collectAsStateWithLifecycle()
    val libraryBadge by viewModel.libraryBadgeCount.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .padding(bottom = 24.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.testTag("expressive_nav_bar")
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainDestination.entries.forEach { destination ->
                    val isSelected = destination == currentDestination
                    val badgeCount = when (destination) {
                        MainDestination.QUEUE -> queueBadge
                        MainDestination.LIBRARY -> libraryBadge
                        else -> 0
                    }

                    ExpressiveNavItem(
                        destination = destination,
                        isSelected = isSelected,
                        badgeCount = badgeCount,
                        onClick = { viewModel.updateDestination(destination) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveNavItem(
    destination: MainDestination,
    isSelected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "container_color"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "content_color"
    )

    val itemWidth by animateDpAsState(
        targetValue = if (isSelected) 110.dp else 64.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "item_width"
    )

    Box(
        modifier = Modifier
            .height(52.dp)
            .width(itemWidth)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Custom indication handled by background animation
                onClick = onClick
            )
            .testTag("nav_item_${destination.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = if (destination == MainDestination.QUEUE) NeonCyan else MaterialTheme.colorScheme.primary,
                            contentColor = if (destination == MainDestination.QUEUE) Color.Black else Color.White,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text("$badgeCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                    contentDescription = destination.title,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            if (isSelected) {
                Text(
                    text = destination.title,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1
                )
            }
        }
    }
}
