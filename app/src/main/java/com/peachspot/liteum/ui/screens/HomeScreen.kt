package com.peachspot.liteum.ui.screens

import android.app.Application
// import android.content.Intent // 현재 코드에서 직접 사용하지 않음
// import android.net.Uri // 현재 코드에서 직접 사용하지 않음
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.ai.client.generativeai.Chat
import com.google.firebase.auth.FirebaseAuth
import com.peachspot.liteum.R
import com.peachspot.liteum.data.db.NotificationEntity
import com.peachspot.liteum.viewmodel.AuthUiState // ViewModel의 uiState를 사용하기 위해
import com.peachspot.liteum.viewmodel.HomeViewModel
import com.peachspot.liteum.viewmodel.NotificationViewModel
import com.peachspot.liteum.viewmodel.NotificationViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// import com.peachspot.liteum.util.Logger // Logger는 필요에 따라 사용


// --- 데이터 클래스 정의 ---
data class BookReview(
    val id: String,
    val reviewerName: String,
    val reviewText: String,
    val rating: Float // 0.0 ~ 5.0
)

data class FeedItem(
    val id: String,
    val userName: String,
    val userProfileImageUrl: String?,
    val postImageUrl: String, // 책 표지 이미지 URL
    val bookTitle: String, // 책 제목
    val caption: String,
    val likes: Int,
    val comments: Int,
    val timestamp: Long,
    val reviews: List<BookReview> = emptyList() // 해당 책의 리뷰 목록
)

// --- Composable 함수들 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current // 현재 컨텍스트
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) } // 계정 관리 드롭다운용

    val application = LocalContext.current.applicationContext as Application
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(application)
    )
    val notifications by notificationViewModel.notifications.collectAsState()

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(uiState.isEnding) {
        if (uiState.isEnding) {
            navController.navigate("byebye") {
                launchSingleTop = true
            }
        }
    }

    if (leftDrawerState.isOpen) {
        BackHandler { scope.launch { leftDrawerState.close() } }
    }
    if (rightDrawerState.isOpen) {
        BackHandler { scope.launch { rightDrawerState.close() } }
    }

    val leftDrawerContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { scope.launch { leftDrawerState.close() } },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "닫기")
                }
            }
            Spacer(Modifier.height(10.dp))
            Image(
                painter = painterResource(id = R.drawable.round_liteum), // 앱 로고 또는 사용자 프로필
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(16.dp))
            Text(uiState.userName ?: "사용자 이름", style = MaterialTheme.typography.titleMedium)
            Text(uiState.userEmail ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = { /* 설정 화면으로 이동 */ }) { Text("설정") }
            TextButton(onClick = { /* 저장된 항목 화면으로 이동 */ }) { Text("저장됨") }
            TextButton(onClick = {
                scope.launch {
                    leftDrawerState.close()
                    viewModel.logOut()
                }
            }) { Text("로그아웃") }

            Spacer(Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                    ) { Text("계정 관리") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("계정 삭제") }, onClick = {
                            expanded = false
                            viewModel.signOut()
                        })
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Powered by Peachspot",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    val rightDrawerContent: @Composable () -> Unit = {
        Column(modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("알림", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { notificationViewModel.clearNotifications() }) {
                            Icon(Icons.Filled.Delete, "모든 알림 삭제")
                        }
                    }
                    IconButton(onClick = { scope.launch { rightDrawerState.close() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "닫기")
                    }
                }
            }
            Divider()
            if (notifications.isEmpty()) {
                Box(Modifier
                    .fillMaxSize()
                    .padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("알림이 없습니다")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationItem(notification = notification)
                    }
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerContent = { if (uiState.isUserLoggedIn) rightDrawerContent() },
        drawerState = rightDrawerState,
        gesturesEnabled = uiState.isUserLoggedIn && rightDrawerState.isOpen
    ) {
        ModalNavigationDrawer(
            drawerContent = { if (uiState.isUserLoggedIn) leftDrawerContent() },
            drawerState = leftDrawerState,
            gesturesEnabled = uiState.isUserLoggedIn && leftDrawerState.isOpen
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    InstagramTopAppBar(
                        onCameraClick = { /* TODO: 카메라 실행 로직 */ },
                        onDmClick = {
                            scope.launch {
                                if (rightDrawerState.isClosed) rightDrawerState.open()
                                else rightDrawerState.close()
                            }
                        },
                        onMenuClick = if (uiState.isUserLoggedIn) {
                            { scope.launch { if (leftDrawerState.isClosed) leftDrawerState.open() else leftDrawerState.close() } }
                        } else null
                    )
                },
            ) { innerPadding ->
                // --- 샘플 피드 데이터 ---
                val feedItems = remember {
                    listOf(
                        FeedItem(
                            "1", "peachspot", "https://picsum.photos/seed/peachspot/100/100",
                            "https://picsum.photos/seed/book_cover1/600/800",
                            "멋진 하루를 만드는 작은 습관",
                            "첫 번째 게시물입니다! 이 책 정말 좋아요. #일상 #책추천", 120, 15,
                            System.currentTimeMillis() - 100000,
                            reviews = listOf(
                                BookReview("r1_1", "독서광1", "정말 감명 깊게 읽었습니다. 삶의 태도를 바꾸는 계기가 되었어요.", 4.5f),
                                BookReview("r1_2", "책벌레", "내용이 조금 어렵긴 했지만, 곱씹을수록 좋은 책입니다.", 4.0f)
                            )
                        ),
                        FeedItem(
                            "2", "tester_01", null,
                            "https://picsum.photos/seed/book_cover2/600/700",
                            "코딩의 정석: 파이썬 기초",
                            "파이썬 입문용으로 최고! #코딩 #개발", 250, 30,
                            System.currentTimeMillis() - 200000,
                            reviews = listOf(
                                BookReview("r2_1", "개발자지망생", "쉽고 재미있게 설명해줘서 좋았습니다. 추천!", 5.0f)
                            )
                        ),
                        FeedItem(
                            "3", "android_dev", "https://picsum.photos/seed/android/100/100",
                            "https://picsum.photos/seed/book_cover3/600/750",
                            "안드로이드 앱 개발 완벽 가이드",
                            "새로운 기능을 개발 중입니다. 이 책 참고하고 있어요! #개발 #안드로이드", 88, 12,
                            System.currentTimeMillis() - 300000,
                            reviews = emptyList()
                        )
                    )
                }

                FeedList(
                    feedItems = feedItems,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstagramTopAppBar(
    onCameraClick: () -> Unit,
    onDmClick: () -> Unit,
    onMenuClick: (() -> Unit)?
) {
    TopAppBar(
        title = {
            Image(
                painter = painterResource(id = R.drawable.round_liteum), // 앱 로고로 변경
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.height(30.dp)
            )
        },
        navigationIcon = {
            onMenuClick?.let { clickAction ->
                IconButton(onClick = clickAction) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = "메뉴")
                }
            }
        },
        actions = {
            IconButton(onClick = onCameraClick) {
                Icon(Icons.Filled.Add, contentDescription = "새 게시물")
            }
            IconButton(onClick = onDmClick) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "DM")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun FeedList(
    feedItems: List<FeedItem>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(feedItems, key = { it.id }) { feedItem ->
            FeedPostItem(feedItem = feedItem)
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FeedPostItem(feedItem: FeedItem, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { 2 }) // 0: 책 이미지, 1: 리뷰 목록
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = feedItem.userProfileImageUrl ?: R.drawable.default_profile_placeholder,
                contentDescription = "${feedItem.userName} 프로필 사진",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = feedItem.userName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /* TODO: 더보기 메뉴 */ }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "더보기")
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
                            model = feedItem.postImageUrl,
                            contentDescription = "${feedItem.bookTitle} 표지",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    1 -> {
                        ReviewList(
                            bookTitle = feedItem.bookTitle,
                            reviews = feedItem.reviews,
                            modifier = Modifier.fillMaxSize()
                        )
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
                    text = { Text("책 표지") }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("리뷰 (${feedItem.reviews.size})") }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: 좋아요 */ }) {
                Icon(Icons.Filled.FavoriteBorder, contentDescription = "좋아요")
            }
            IconButton(onClick = { /* TODO: 댓글 */ }) {
                Icon(
                    painter = painterResource(R.drawable.ic_message),
                    contentDescription = "댓글"
                )

            }

            IconButton(onClick = { /* TODO: 공유 */ }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "공유")
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Text(
            text = "좋아요 ${feedItem.likes}개",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(modifier = Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = feedItem.userName,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = feedItem.caption,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        if (feedItem.comments > 0) {
            Text(
                text = "댓글 ${feedItem.comments}개 모두 보기",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = formatTimestamp(feedItem.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ReviewList(bookTitle: String, reviews: List<BookReview>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        Text(
            text = "\"${bookTitle}\" 리뷰",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp)
        )
        Divider()
        if (reviews.isEmpty()) {
            Box(Modifier
                .fillMaxSize()
                .padding(16.dp), contentAlignment = Alignment.Center) {
                Text("아직 작성된 리뷰가 없습니다.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reviews, key = { it.id }) { review ->
                    BookReviewItem(review)
                }
            }
        }
    }
}

@Composable
fun BookReviewItem(review: BookReview, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = review.reviewerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⭐️ ${"%.1f".format(review.rating)}", // 실제 별 아이콘 사용 권장
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = review.reviewText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "${days}일 전"
        hours > 0 -> "${hours}시간 전"
        minutes > 0 -> "${minutes}분 전"
        else -> "${seconds}초 전"
    }
}

@Composable
fun NotificationItem(notification: NotificationEntity, modifier: Modifier = Modifier) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()) }
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()) {
            notification.imgUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = notification.title ?: "알림 이미지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            notification.title?.let { title ->
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
            }
            notification.body?.let { body ->
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(text = dateFormat.format(Date(notification.receivedTimeMillis)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// --- Preview 코드 ---
@Preview(showBackground = true, widthDp = 360, heightDp = 1000)@Composable
fun HomeScreenPreview() {
    val context = LocalContext.current
}
    @Composable
    fun FeedPostItemPreview() {
        MaterialTheme {
            FeedPostItem(
                feedItem = FeedItem(
                    id = "preview1",
                    userName = "instagram_user",
                    userProfileImageUrl = "https://picsum.photos/seed/preview_profile/100/100",
                    postImageUrl = "https://picsum.photos/seed/preview_post/600/600",
                    bookTitle = "미리보기 책 제목",
                    caption = "이것은 미리보기용 게시물입니다. #Compose #AndroidDev",
                    likes = 1052,
                    comments = 37,
                    timestamp = System.currentTimeMillis() - 3600000, // 1시간 전
                    reviews = listOf(
                        BookReview("r_prev1", "리뷰어1", "아주 좋은 책입니다. 내용이 알차요!", 5.0f),
                        BookReview("r_prev2", "리뷰어2", "읽어볼 만합니다.", 4.0f)
                    )
                )
            )
        }
    }


// drawable에 instagram_logo_text.xml (또는 round_liteum.xml) 및 default_profile_placeholder.xml 필요
