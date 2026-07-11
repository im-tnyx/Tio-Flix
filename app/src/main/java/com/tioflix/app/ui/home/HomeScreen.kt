package com.tioflix.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tioflix.app.domain.model.ContentCategory
import com.tioflix.app.domain.model.ContentItem
import com.tioflix.app.domain.model.ContentType

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> LoadingState()
            state.errorMessage != null -> ErrorState(
                message = state.errorMessage,
                onRetry = { onAction(HomeAction.RetryClicked) }
            )
            state.catalog == null || state.catalog.categories.all { it.items.isEmpty() } -> EmptyState()
            else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                val isTvLayout = maxWidth >= 720.dp
                val horizontalPadding = if (isTvLayout) 48.dp else 20.dp
                val posterWidth = if (isTvLayout) 180.dp else 132.dp

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(if (isTvLayout) 28.dp else 20.dp)
                ) {
                    item {
                        Header(
                            title = state.title,
                            onLogout = { onAction(HomeAction.LogoutClicked) },
                            horizontalPadding = horizontalPadding
                        )
                    }

                    state.catalog.featured?.let { featured ->
                        item {
                            HeroBanner(
                                item = featured,
                                horizontalPadding = horizontalPadding,
                                isTvLayout = isTvLayout,
                                onClick = { onAction(HomeAction.ContentClicked(featured.id)) }
                            )
                        }
                    }

                    items(state.catalog.categories, key = { it.id }) { category ->
                        if (category.items.isNotEmpty()) {
                            ContentRow(
                                category = category,
                                horizontalPadding = horizontalPadding,
                                posterWidth = posterWidth,
                                onItemClick = { onAction(HomeAction.ContentClicked(it.id)) }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, onLogout: () -> Unit, horizontalPadding: androidx.compose.ui.unit.Dp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Button(onClick = onLogout) { Text("Logout") }
    }
}

@Composable
private fun HeroBanner(
    item: ContentItem,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    isTvLayout: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .height(if (isTvLayout) 360.dp else 230.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .focusable()
    ) {
        AsyncImage(
            model = item.backdropUrl ?: item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f)))
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(if (isTvLayout) 32.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(item.title, style = if (isTvLayout) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(contentMeta(item), style = MaterialTheme.typography.labelLarge)
            item.description?.let {
                Text(it, maxLines = if (isTvLayout) 3 else 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.75f))
            }
            Button(onClick = onClick) { Text(if (item.type == ContentType.SERIES) "View episodes" else "Play") }
        }
    }
}

@Composable
private fun ContentRow(
    category: ContentCategory,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    posterWidth: androidx.compose.ui.unit.Dp,
    onItemClick: (ContentItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            category.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(category.items, key = { it.id }) { item ->
                PosterCard(item = item, width = posterWidth, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun PosterCard(item: ContentItem, width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.07f else 1f, label = "posterFocus")

    Column(
        modifier = Modifier
            .width(width)
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (focused) 12.dp else 2.dp)
        ) {
            Box {
                AsyncImage(
                    model = item.posterUrl ?: item.backdropUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxWidth().height(width * 1.5f),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = if (item.type == ContentType.SERIES) "SERIES" else "MOVIE",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }
        }
        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
        Text(contentMeta(item), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
    }
}

private fun contentMeta(item: ContentItem): String = buildList {
    item.releaseYear?.let { add(it.toString()) }
    item.maturityRating?.takeIf { it.isNotBlank() }?.let(::add)
    when (item.type) {
        ContentType.MOVIE -> item.durationMinutes?.let { add("${it}m") }
        ContentType.SERIES -> item.totalSeasons?.let { add("$it season${if (it == 1) "" else "s"}") }
    }
}.joinToString(" • ")

@Composable
private fun LoadingState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
    }
}

@Composable
private fun EmptyState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text("No published movies or series yet.", style = MaterialTheme.typography.bodyLarge)
}
