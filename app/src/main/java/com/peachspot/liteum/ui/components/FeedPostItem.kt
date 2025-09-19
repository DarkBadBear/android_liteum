package com.peachspot.liteum.ui.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.peachspot.liteum.R // R 경로 확인 필요
import com.peachspot.liteum.data.model.BookReview
import com.peachspot.liteum.data.model.FeedItem
import com.peachspot.liteum.util.formatTimestamp // 분리된 유틸리티 함수 사용
import com.peachspot.liteum.viewmodel.FeedViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedPostItem(
    feedItem: FeedItem,
    onItemClick: (FeedItem) -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: (review: BookReview?) -> Unit,
    onDeleteClick: (review: BookReview?) -> Unit,
    feedViewModel: FeedViewModel = viewModel() // ViewModel 주입
){
    var showReviewDialog by remember { mutableStateOf(false) }
    val currentReview = feedItem.reviews.firstOrNull() // 예시로 첫 번째 리뷰를 사용, 실제로는 어떤 리뷰를 보여줄지 로직 필요
    var showMenu by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 2 }) // 0: 책 이미지, 1: 리뷰 목록
    val scope = rememberCoroutineScope()


    val feedItemIdentifier = feedItem.isbn ?: feedItem.id // ISBN이 없다면 ID 사용 (FeedItem에 id 필드가 있다고 가정)

    val externalReviewsState by feedViewModel.externalReviews.collectAsState()
    val currentExternalReviewState = externalReviewsState[feedItemIdentifier]

    LaunchedEffect(pagerState, feedItem.isbn) { // feedItem.isbn도 key로 추가하여 isbn이 변경될 경우 재실행
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            if (page == 1 && feedItem.isbn != null) {
                Log.d("FeedPostItem", "Page 1 settled for ISBN: ${feedItem.isbn}. Fetching external reviews.")
                feedViewModel.fetchExternalReviews(feedItemIdentifier, feedItem.isbn)
            }
        }
    }



    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            AsyncImage(
//                model = feedItem.userProfileImageUrl ?: R.drawable.default_profile_placeholder, // 플레이스홀더 리소스 필요
//                contentDescription = "${feedItem.userName} 프로필 사진",
//                modifier = Modifier
//                    .size(32.dp)
//                    .clip(CircleShape)
//                    .background(Color.LightGray),
//                contentScale = ContentScale.Crop
//            )
            //Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = feedItem.userName,
//                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
//                modifier = Modifier.weight(1f)
//            )
            Spacer(modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "더보기 옵션")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("수정") },
                        onClick = {
                            showMenu = false
                            onEditClick(currentReview)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("삭제") },
                        onClick = {
                            showMenu = false
                            onDeleteClick(currentReview)
                        }
                    )
                }
            }
        }


        // 페이지 전환 완료 시 실행될 로직
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                // 페이지 전환이 완료될 때마다 이 블록이 실행됩니다.

                // 여기에 원하는 동작을 추가하세요.
                // 예: if (page == 1) { /* 리뷰 목록으로 전환 완료 시 특정 작업 수행 */ }
            }
        }

        Column {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) { page ->

                when (page) {
                    0 -> {
                        AsyncImage(
                            model = feedItem.bookImageUrl,
                            contentDescription = "${feedItem.bookTitle} 표지",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onItemClick(feedItem) },
                            contentScale = ContentScale.Crop
                        )
                    }
                    1 -> {

                        // 로컬 리뷰와 외부 서버 리뷰를 함께 표시하거나,
                        // 외부 서버 리뷰만 표시할 수 있습니다.
                        // 여기서는 로컬 리뷰와 외부 리뷰를 합쳐서 보여주는 예시입니다.
                        val combinedReviews = mutableListOf<BookReview>()
                        combinedReviews.addAll(feedItem.reviews) // 기존 로컬 리뷰
                        currentExternalReviewState?.reviews?.let { external ->
                            combinedReviews.addAll(external) // 외부에서 가져온 리뷰
                        }

                        if (currentExternalReviewState?.loading == true) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (currentExternalReviewState?.error != null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("리뷰를 불러오는 중 오류 발생: ${currentExternalReviewState.error}")
                            }
                        } else {
                            ReviewList(
                                bookTitle = feedItem.bookTitle,
                                reviews = combinedReviews.distinctBy { it.id }, // ID로 중복 제거
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
//                        ReviewList(
//                            // ReviewList도 분리된 파일에서 import
//                            bookTitle = feedItem.bookTitle,
//                            reviews = feedItem.reviews,
//                            modifier = Modifier.fillMaxSize(),
//                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                 //   text = { Text("책 표지") }
                    modifier = Modifier.height(4.dp) // 탭의 높이를 0으로 설정

                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                   // text = { Text("리뷰 (${feedItem.reviews.size})") }
                    modifier = Modifier.height(4.dp) // 탭의 높이를 0으로 설정

                )
            }
        }


        Row(modifier = Modifier.padding(horizontal = 12.dp)) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    ) {
                Row {
                    Text(
                        text = feedItem.bookTitle,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row {
//                    Text(
//                        text = feedItem.caption, // 또는 currentReview?.content
//                        style = MaterialTheme.typography.bodyMedium,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis
//                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))

//        if (feedItem.comments > 0) {
//            Text(
//                text = "댓글 ${feedItem.comments}개 모두 보기",
//                style = MaterialTheme.typography.bodySmall,
//                color = Color.Gray,
//                modifier = Modifier.padding(horizontal = 12.dp)
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//        }

        Text(
            text = formatTimestamp(feedItem.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )

    }
    }


