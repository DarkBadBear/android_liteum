// com/peachspot/liteum/ui/components/BookGridFeed.kt (예시 경로)
package com.peachspot.liteum.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peachspot.liteum.data.model.FeedItem

@Composable
fun BookGridFeed(
    feedItems: List<FeedItem>,
    onItemClick: (FeedItem) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3 // 그리드 컬럼 수
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp), // 아이템 간 간격 및 전체 패딩
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(feedItems, key = { it.id }) { feedItem ->
            BookCoverGridItem(
                feedItem = feedItem,
                onItemClick = onItemClick
            )
        }
    }
}
   
