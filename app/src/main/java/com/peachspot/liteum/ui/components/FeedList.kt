package com.peachspot.liteum.ui.components // FeedList.kt가 있는 패키지

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState // import 추가
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState // import 추가
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.peachspot.liteum.data.model.BookReview
import com.peachspot.liteum.data.model.FeedItem

// FeedList.kt (예시)
@Composable
fun FeedList(
    feedItems: List<FeedItem>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onItemClick: (FeedItem) -> Unit,
    // HomeScreen의 다이얼로그를 띄우기 위한 콜백 (FeedItem과 Review 함께 전달)
    //onOpenDialogRequest: (feedItem: FeedItem, review: BookReview) -> Unit,
    // FeedPostItem의 자체 메뉴 등에서 직접 삭제를 위한 콜백 (FeedId와 ReviewId 함께 전달)
    //onDeleteActionFromItemMenu: ((feedId: String, reviewId: String) -> Unit)?
    // navControllerForFeedItem: NavController? = null // 필요하다면 전달
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(feedItems, key = { it.id }) { feedItem ->
            // FeedPostItem에 필요한 콜백들 전달
            FeedPostItem(
                feedItem = feedItem,
                onItemClick = onItemClick

//                onOpenDialogRequest = { clickedReview ->
//                    onOpenDialogRequest(feedItem, clickedReview)
//                },
//                onDeleteActionFromItemMenu = onDeleteActionFromItemMenu?.let { callback ->
//                    { reviewId -> callback(feedItem.id, reviewId) }
//                }

            )
            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}
