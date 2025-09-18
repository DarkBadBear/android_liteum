package com.peachspot.liteum.ui.screens

// 필요한 import문들 (분리된 컴포저블, ViewModel, 데이터 클래스 등)
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.peachspot.liteum.R
import com.peachspot.liteum.data.db.NotificationEntity // NotificationEntity 경로에 맞게 수정 필요
import com.peachspot.liteum.data.model.FeedItem // 분리된 FeedItem 사용
import com.peachspot.liteum.data.model.BookReview // 분리된 BookReview 사용
import com.peachspot.liteum.ui.components.AppBottomNavigationBar
import com.peachspot.liteum.ui.components.BookGridFeed
import com.peachspot.liteum.ui.components.FeedList
import com.peachspot.liteum.ui.components.TopAppBar
import com.peachspot.liteum.ui.components.NotificationItem
import com.peachspot.liteum.viewmodel.HomeViewModel
import com.peachspot.liteum.viewmodel.NotificationViewModel
import com.peachspot.liteum.viewmodel.NotificationViewModelFactory
import kotlinx.coroutines.launch

import  com.peachspot.liteum.viewmodel.FeedViewModel

// HomeScreen.kt 또는 Preview들을 모아두는 별도의 파일

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column // Column 추가
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu // 예시 아이콘
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api // 추가
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar // TopAppBar import
import androidx.compose.material3.TopAppBarDefaults // TopAppBarDefaults import
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // getValue, mutableStateOf, remember, setValue 추가
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp // dp import
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.rememberNavController // rememberNavController는 이미 있었음
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.peachspot.liteum.ui.components.AppBottomNavigationBar
import com.peachspot.liteum.ui.components.ReviewPreviewDialog


enum class BottomNavItem(
    val label: String,
    val icon: @Composable () -> Painter // Painter를 반환하는 Composable 함수로 변경
) {
    Home(
        label = "나의 책장",
        icon = { painterResource(id = R.drawable.ic_book) } // 예시: ic_home 드로어블 사용
    ),
    Street(
        label = "북 이음",
        icon = { painterResource(id = R.drawable.ic_link) } // 예시: ic_street 드로어블 사용
    ),
    Profile(
        label = "프로필",
        icon = { painterResource(id = R.drawable.ic_profile) } // 예시: ic_profile 드로어블 사용
    )
    // 필요에 따라 더 많은 아이템 추가
}

enum class ViewMode {
    LIST, GRID
}

enum class PreviewViewMode { LIST, GRID } // Preview용으로 간단히 정의하거나 실제 ViewMode import

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    feedViewModel: FeedViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
) {

    var currentViewMode by remember { mutableStateOf(ViewMode.LIST) } // 기본값은 리스트 뷰

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) } // 계정 관리 드롭다운용
    var showFeedItemDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val application = LocalContext.current.applicationContext as Application
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(application)
    )
    val notifications by notificationViewModel.notifications.collectAsState()
    var selectedBottomNavItem by remember { mutableStateOf(BottomNavItem.Home) } // BottomNavItem 사용


    // LazyColumn의 스크롤 상태를 위한 State
    val listState = rememberLazyListState()

    // 하단 바 표시 여부를 위한 State
    var isBottomBarVisible by remember { mutableStateOf(true) }

    // 이전 스크롤 오프셋을 저장하기 위한 변수 (스크롤 방향 감지용)
    var previousScrollOffset by remember { mutableStateOf(0) }

    // 스크롤 위치를 기반으로 하단 바 표시 여부 결정
    // derivedStateOf를 사용하면 listState.firstVisibleItemScrollOffset이 변경될 때만 재계산됩니다.

    val shouldShowBottomBar by remember {
        derivedStateOf {
            // 가장 간단한 방법: 첫 아이템이 완벽히 보이면 항상 표시
            // 또는 스크롤이 멈췄을 때 표시 등 다양한 로직 구현 가능
            // 여기서는 스크롤 방향에 따라 결정하는 로직을 추가합니다.
            val currentOffset = listState.firstVisibleItemScrollOffset
            val firstVisibleItemIndex = listState.firstVisibleItemIndex

            // 첫 아이템이 보이거나, 스크롤을 위로 올렸을 때
            if (firstVisibleItemIndex == 0 && currentOffset == 0) {
                true // 맨 위에서는 항상 표시
            } else if (currentOffset > previousScrollOffset) {
                false // 아래로 스크롤 중이면 숨김
            } else if (currentOffset < previousScrollOffset) {
                true // 위로 스크롤 중이면 표시
            } else {

                isBottomBarVisible // 스크롤이 멈췄으면 이전 상태 유지 (또는 true로 설정)
            }
        }
    }

  var  selectedFeedItemForDialog by remember { mutableStateOf<FeedItem?>(null) }
    var reviewForDialog by remember { mutableStateOf<BookReview?>(null) } // 다이얼로그에 표시할 특정 리뷰

    // 다이얼로그를 띄우는 공통 함수
    fun openDialogWithFeedItem(feedItem: FeedItem, specificReview: BookReview?) {
        selectedFeedItemForDialog = feedItem
        reviewForDialog = specificReview // 특정 리뷰가 있으면 사용, 없으면 null
        showFeedItemDialog = true
    }

    // 다이얼로그를 띄우는 함수 (리뷰가 필수인 경우, 없으면 Snackbar)
    fun openDialogRequiringReview(feedItem: FeedItem) {
        val targetReview = feedItem.reviews.firstOrNull() // 예시: 첫 번째 리뷰를 대상으로 함
        if (targetReview != null) {
            openDialogWithFeedItem(feedItem, targetReview)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "${feedItem.bookTitle}에는 표시할 리뷰가 없습니다.",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // shouldShowBottomBar 값이 변경될 때 isBottomBarVisible 상태를 업데이트
    // 그리고 previousScrollOffset 업데이트
    LaunchedEffect(shouldShowBottomBar, listState.firstVisibleItemScrollOffset) {
        Log.d("ScrollDebug", "Effect triggered. shouldShow: $shouldShowBottomBar, currentOffset: ${listState.firstVisibleItemScrollOffset}, prevOffset: $previousScrollOffset")
        isBottomBarVisible = shouldShowBottomBar
        previousScrollOffset = listState.firstVisibleItemScrollOffset
        Log.d("ScrollDebug", "isBottomBarVisible set to: $isBottomBarVisible, previousScrollOffset set to: $previousScrollOffset")
    }

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
                    Icon(imageVector = Icons.Default.Close, contentDescription = "닫기",    tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(10.dp))
            Image(
                painter = painterResource(id = R.drawable.round_liteum),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(16.dp))
            Text(uiState.userName ?: "사용자 이름", style = MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)


            Spacer(Modifier.height(24.dp))

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        leftDrawerState.close()
                        val url = "https://peachspot.co.kr/blog/detail?no=2"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFefefef),
                    contentColor = Color.Black
                ),
                modifier = Modifier.width(200.dp) // 원하는 너비로 설정
            ) { Text("웹사이트") }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        leftDrawerState.close()
                        val url = "https://peachspot.co.kr/privacy?no=2"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFefefef),
                    contentColor = Color.Black
                ),  modifier = Modifier.width(200.dp) // 원하는 너비로 설정
            ) { Text("개인정보취급방침") }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        leftDrawerState.close()
                        viewModel.logOut() // <- 상태 변경만 수행
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFefefef),
                    contentColor = Color.Black
                ),
                modifier = Modifier.width(200.dp) // 원하는 너비로 설정
            ) { Text("로그아웃") }

            Spacer(Modifier.height(24.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFefefef),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.width(200.dp) // 원하는 너비로 설정
                    ) { Text("계정 관리") }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("계정 삭제") }, onClick = {
                            expanded = false
                            // 계정 삭제 처리
                        })
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Powered by Peachspot",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top=16.dp,bottom = 16.dp)
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
                        NotificationItem(notification = notification) // 분리된 컴포저블 사용
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
                    TopAppBar( // 분리된 컴포저블 사용
                        currentViewMode = currentViewMode, // 현재 뷰 모드 전달
                        onViewModeChange = { newMode -> // 뷰 모드 변경을 위한 콜백 전달
                            currentViewMode = newMode
                        },
                        onCameraClick = {
                            navController.navigate("review") // 여기서 네비게이션 실행
                        },
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
                bottomBar = {
                    AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = slideInVertically(
                            // initialOffsetY는 컴포저블이 화면 밖에서 시작하여 안으로 들어올 때의 시작점입니다.
                            // 양수 값은 컴포저블의 아래쪽을 의미합니다.
                            // 따라서, 컴포저블 높이만큼 아래에서 시작하면 완전히 화면 밖에 있게 됩니다.
                            // 절반만 움직이게 하려면, 컴포저블 높이의 절반만큼만 오프셋을 줍니다.
                            initialOffsetY = { fullHeight -> fullHeight }, // 시작: 완전히 화면 아래
                            animationSpec = tween(
                                durationMillis = 300, // 애니메이션 지속 시간 (밀리초)
                                easing = androidx.compose.animation.core.FastOutSlowInEasing // 부드러운 가속/감속
                            )
                        ),
                        exit = slideOutVertically(
                            // targetOffsetY는 컴포저블이 화면 안에서 밖으로 나갈 때의 목표점입니다.
                            // 양수 값은 컴포저블의 아래쪽을 의미합니다.
                            targetOffsetY = { fullHeight -> fullHeight }, // 종료: 완전히 화면 아래
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = androidx.compose.animation.core.FastOutLinearInEasing // 사라질 때는 약간 더 빠르게
                            )
                        )
                    ) {
                        // 이 AppBottomNavigationBar가 차지하는 높이가 fullHeight가 됩니다.
                        AppBottomNavigationBar(
                            selectedItem = selectedBottomNavItem,
                            onItemSelected = { item ->
                                selectedBottomNavItem = item
                                // TODO: 네비게이션 로직
                            }
                        )
                    }
                }

            ) { innerPadding ->
                // --- 샘플 피드 데이터 ---
//                val feedItems = remember {
//                    listOf(
//                        FeedItem(
//                            "1", "peachspot", "https://picsum.photos/seed/peachspot/100/100",
//                            "https://picsum.photos/seed/book_cover1/600/800",
//                            "멋진 하루를 만드는 작은 습관",
//                            "첫 번째 게시물입니다! 이 책 정말 좋아요. #일상 #책추천", 120, 15,
//                            System.currentTimeMillis() - 100000,
//                            reviews = listOf(
//                                BookReview("1", "1234","독서광1", "정말 감명 깊게 읽었습니다. 삶의 태도를 바꾸는 계기가 되었어요.", 4.5f,""),
//
//                            )
//                        ),
//                        FeedItem(
//                            "2", "tester_01", null,
//                            "https://picsum.photos/seed/book_cover2/600/700",
//                            "코딩의 정석: 파이썬 기초",
//                            "파이썬 입문용으로 최고! #코딩 #개발", 250, 30,
//                            System.currentTimeMillis() - 200000,
//                            reviews = listOf(
//                                BookReview("1","123", "개발자지망생", "쉽고 재미있게 설명해줘서 좋았습니다. 추천!", 5.0f,"")
//                            )
//                        ),
//                        FeedItem(
//                            "3", "android_dev", "https://picsum.photos/seed/android/100/100",
//                            "https://picsum.photos/seed/book_cover3/600/750",
//                            "안드로이드 앱 개발 완벽 가이드",
//                            "새로운 기능을 개발 중입니다. 이 책 참고하고 있어요! #개발 #안드로이드", 88, 12,
//                            System.currentTimeMillis() - 300000,
//                            reviews = emptyList()
//                        )
//                    )
//                }

                val feedItems: LazyPagingItems<FeedItem> = feedViewModel.feedItemsPager.collectAsLazyPagingItems()



                when (currentViewMode) {
                    ViewMode.LIST -> {
                        FeedList(
                            feedItems = feedItems,
                            navController ,
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            listState = listState,
                            onItemClick = { clickedFeedItemNullable -> // 파라미터가 FeedItem? 임을 명시
                                // 여기서 null 체크 후 non-null 타입으로 openDialogRequiringReview 호출
                                clickedFeedItemNullable?.let { nonNullFeedItem ->
                                    openDialogRequiringReview(nonNullFeedItem)
                                }
                                // 또는, null일 경우 아무것도 안 하거나 다른 처리를 할 수 있음
                                // if (clickedFeedItemNullable != null) {
                                //     openDialogRequiringReview(clickedFeedItemNullable)
                                // }
                            }
                        )
                    }
                    ViewMode.GRID -> {
                        BookGridFeed(
                            feedItems = feedItems,
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            onItemClick = { clickedFeedItemNullable -> // 파라미터가 FeedItem? 임을 명시
                                // 여기서 null 체크 후 non-null 타입으로 openDialogRequiringReview 호출
                                clickedFeedItemNullable?.let { nonNullFeedItem ->
                                    openDialogRequiringReview(nonNullFeedItem)
                                }
                            }
                        )
                    }
                }


                // --- 공통으로 사용할 다이얼로그 ---
                if (showFeedItemDialog && selectedFeedItemForDialog != null && reviewForDialog != null) {
                    ReviewPreviewDialog(
                        feedItem = selectedFeedItemForDialog!!,
                        review = reviewForDialog!!,
                        onDismissRequest = {
                            showFeedItemDialog = false
                            // 상태 초기화
                            selectedFeedItemForDialog = null
                            reviewForDialog = null
                        },
                        onEditClick = {
                            showFeedItemDialog = false

                            reviewForDialog?.let { review ->
                                navController.navigate("review_edit/${review.id}")
                            }
                            selectedFeedItemForDialog = null
                            reviewForDialog = null
                        },
                        onDeleteClick = {
                            showFeedItemDialog = false
                            // viewModel.deleteReview(selectedFeedItemForDialog!!.id, reviewForDialog!!.id) // ViewModel 호출
                            println("삭제 요청 (Dialog): Feed ID - ${selectedFeedItemForDialog!!.id}, Review ID - ${reviewForDialog!!.id}")
                            scope.launch {
                                snackbarHostState.showSnackbar("리뷰가 삭제되었습니다. (ViewModel 연동 필요)")
                            }
                            selectedFeedItemForDialog = null
                            reviewForDialog = null
                        }
                    )
                } else if (showFeedItemDialog && selectedFeedItemForDialog != null && reviewForDialog == null) {

                    LaunchedEffect(selectedFeedItemForDialog) {
                        if(showFeedItemDialog) { // 다이얼로그를 닫기 전에 Snackbar 표시
                            scope.launch {
                                snackbarHostState.showSnackbar("${selectedFeedItemForDialog?.bookTitle}에는 표시할 리뷰가 없습니다.")
                            }
                            showFeedItemDialog = false // 다이얼로그 닫기
                            selectedFeedItemForDialog = null
                        }
                    }
                }
            }
        }
    }
}


