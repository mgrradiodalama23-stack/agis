package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AtlasCrimson
import com.example.ui.theme.AtlasCrimsonBright
import com.example.ui.theme.AtlasCrimsonDark
import com.example.ui.theme.AtlasOnlineGreen
import com.example.ui.theme.AtlasWarningAmber

@Composable
fun AtlasTopBar(
    isVisible: Boolean,
    isLoading: Boolean,
    loadProgress: Int,
    isOnline: Boolean,
    canGoBack: Boolean,
    onBackClick: () -> Unit,
    onReloadClick: () -> Unit,
    onHomeClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onOpenInBrowserClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onPortalInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            AtlasCrimsonDark,
                            AtlasCrimson,
                            AtlasCrimsonBright
                        )
                    )
                )
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.atlas_icon_fg),
                        contentDescription = stringResource(id = R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ATLAS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.22f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isOnline) AtlasOnlineGreen else AtlasWarningAmber
                                        )
                                )
                                Text(
                                    text = stringResource(
                                        id = if (isOnline) R.string.status_online else R.string.status_offline
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(id = R.string.portal_subtitle),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.9.sp
                        ),
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (canGoBack) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("top_bar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.action_back),
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onReloadClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_bar_reload_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(id = R.string.action_reload),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onFullscreenClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_bar_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Fullscreen,
                        contentDescription = stringResource(id = R.string.action_fullscreen),
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("top_bar_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(id = R.string.action_menu),
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_home)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Home, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onHomeClick()
                            },
                            modifier = Modifier.testTag("menu_home_item")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_open_browser)) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenInBrowserClick()
                            },
                            modifier = Modifier.testTag("menu_open_browser_item")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_clear_cache)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.CleaningServices, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onClearCacheClick()
                            },
                            modifier = Modifier.testTag("menu_clear_cache_item")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.action_portal_info)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Info, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onPortalInfoClick()
                            },
                            modifier = Modifier.testTag("menu_portal_info_item")
                        )
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    progress = { (loadProgress.coerceIn(5, 100)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = Color(0xFFFFD166),
                    trackColor = Color.White.copy(alpha = 0.20f)
                )
            }
        }
    }
}
