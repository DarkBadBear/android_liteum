package com.peachspot.liteum.ui.screens

// 필요한 import문들 (분리된 컴포저블, ViewModel, 데이터 클래스 등)
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import com.peachspot.liteum.ui.components.TopAppBar // TopAppBar import 이름 변경
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
// import androidx.compose.material.icons.filled.Menu // 예시 아이콘 - 이미 위에서 import 됨
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api // 추가
import androidx.compose.material3.HorizontalDivider // 이미 위에서 import 됨
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
// import androidx.compose.material3.TopAppBar // TopAppBar import - 이미 위에서 import 됨
import androidx.compose.material3.TopAppBarDefaults // TopAppBarDefaults import
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // getValue, mutableStateOf, remember, setValue 추가
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
// import androidx.compose.ui.unit.dp // dp import - 이미 위에서 import 됨
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.rememberNavController // rememberNavController는 이미 있었음
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
// import com.peachspot.liteum.ui.components.AppBottomNavigationBar // 이미 위에서 import 됨
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

// Preview용 enum은 별도 파일 또는 삭제 가능
// enum class PreviewViewMode { LIST, GRID }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    feedViewModel: FeedViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
) {

    var currentViewMode by remember { mutableStateOf(ViewMode.LIST) }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) } // 계정 관리 드롭다운용
    var showFeedItemDialog by remember { mutableStateOf(false) } // ReviewPreviewDialog 표시 여부
    val context = LocalContext.current
    val application = LocalContext.current.applicationContext as Application
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(application)
    )
    val notifications by notificationViewModel.notifications.collectAsState()
    var selectedBottomNavItem by remember { mutableStateOf(BottomNavItem.Home) }


    val listState = rememberLazyListState()
    var isBottomBarVisible by remember { mutableStateOf(true) }
    var previousScrollOffset by remember { mutableStateOf(0) }

    val shouldShowBottomBar by remember {
        derivedStateOf {
            val currentOffset = listState.firstVisibleItemScrollOffset
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            if (firstVisibleItemIndex == 0 && currentOffset == 0) {
                true
            } else if (currentOffset > previousScrollOffset) {
                false
            } else if (currentOffset < previousScrollOffset) {
                true
            } else {
                isBottomBarVisible
            }
        }
    }

    // ReviewPreviewDialog를 위한 상태
    var selectedFeedItemForDialog by remember { mutableStateOf<FeedItem?>(null) }
    var reviewForDialog by remember { mutableStateOf<BookReview?>(null) }

    fun openDialogWithFeedItem(feedItem: FeedItem, specificReview: BookReview?) {
        selectedFeedItemForDialog = feedItem
        reviewForDialog = specificReview
        showFeedItemDialog = true
    }

    fun openDialogRequiringReview(feedItem: FeedItem) {
        val targetReview = feedItem.reviews.firstOrNull()
        if (targetReview != null) {
            openDialogWithFeedItem(feedItem, targetReview)
        } else {
            // ReviewPreviewDialog를 띄우지 않고 Snackbar만 표시
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "${feedItem.bookTitle}에는 표시할 리뷰가 없습니다.",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(shouldShowBottomBar, listState.firstVisibleItemScrollOffset) {
        // Log.d("ScrollDebug", "Effect triggered. shouldShow: $shouldShowBottomBar, currentOffset: ${listState.firstVisibleItemScrollOffset}, prevOffset: $previousScrollOffset")
        isBottomBarVisible = shouldShowBottomBar
        previousScrollOffset = listState.firstVisibleItemScrollOffset
        // Log.d("ScrollDebug", "isBottomBarVisible set to: $isBottomBarVisible, previousScrollOffset set to: $previousScrollOffset")
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
                modifier = Modifier.width(200.dp)
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
                ),  modifier = Modifier.width(200.dp)
            ) { Text("개인정보취급방침") }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        leftDrawerState.close()
                        viewModel.logOut()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFefefef),
                    contentColor = Color.Black
                ),
                modifier = Modifier.width(200.dp)
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
                        modifier = Modifier.width(200.dp)
                    ) { Text("계정 관리") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("계정 삭제") }, onClick = {
                            expanded = false
                            // TODO: 계정 삭제 처리 로직
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
            HorizontalDivider() // Material3의 Divider로 변경
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
                    com.peachspot.liteum.ui.components.TopAppBar( // 전체 경로 명시 또는 TopAppBar import 이름 변경
                        currentViewMode = currentViewMode,
                        onViewModeChange = { newMode ->
                            currentViewMode = newMode
                        },
                        onCameraClick = {
                            navController.navigate("review")
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
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing)
                        )
                    ) {
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


                val feedItems: LazyPagingItems<FeedItem> = feedViewModel.feedItemsPager.collectAsLazyPagingItems()

                when (currentViewMode) {
                    ViewMode.LIST -> {

                        FeedList(
                            feedItems = feedItems,
                            navController = navController,
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            listState = listState,
                            onItemClick = { clickedFeedItemNullable ->
                                clickedFeedItemNullable?.let { nonNullFeedItem ->
                                    openDialogRequiringReview(nonNullFeedItem)
                                }
                            },
                            // --- 수정된 콜백 ---
                            // HomeScreen.kt 내부의 FeedList 호출 부분

                            onEditClickCallback = editCallback@{ actualFeedItem, actualReview -> // "editCallback@" 레이블 추가
                                if (showFeedItemDialog && selectedFeedItemForDialog == actualFeedItem && reviewForDialog == actualReview) {
                                    showFeedItemDialog = false
                                    selectedFeedItemForDialog = null
                                    reviewForDialog = null
                                }

                                val targetBookLogId = actualFeedItem.id

                                if (targetBookLogId <= 0L) {
                                    Log.e("HomeScreen", "Cannot navigate to edit: targetBookLogId is invalid for FeedItem: ${actualFeedItem.bookTitle}")
                                    scope.launch {
                                        snackbarHostState.showSnackbar("리뷰를 수정/작성할 수 없는 항목입니다.")
                                    }
                                    // --- 수정된 return 문 ---
                                    return@editCallback // 명시적 레이블 사용
                                }

                                val route = "review_edit/$targetBookLogId"
                                Log.d("HomeScreen", "Navigating to: $route (Review exists: ${actualReview != null})")
                                navController.navigate(route)
                            },

                            onDeleteClickCallback = { actualFeedItem, actualReview ->
                                if (showFeedItemDialog && selectedFeedItemForDialog == actualFeedItem && reviewForDialog == actualReview) {
                                    showFeedItemDialog = false
                                    selectedFeedItemForDialog = null
                                    reviewForDialog = null
                                }
                                val targetBookLogId = actualFeedItem.id
                                if (actualFeedItem.id > 0L) {
                                    viewModel.deleteBookLogWithReviews(targetBookLogId)
                                    Log.d("HomeScreen", "Deleted FeedItem ${actualFeedItem.id} and its reviews")
                                    scope.launch {
                                        snackbarHostState.showSnackbar("책 기록과 리뷰가 삭제되었습니다.")
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("삭제할 항목이 없습니다.")
                                    }
                                    Log.d("HomeScreen", "Delete failed: invalid FeedItem ID")
                                }
                            },

                                    feedViewModel = feedViewModel
                        )
                    }
                    ViewMode.GRID -> {
                        BookGridFeed(
                            feedItems = feedItems,
                            modifier = Modifier.padding(innerPadding).fillMaxSize(),
                            onItemClick = { clickedFeedItemNullable ->
                                clickedFeedItemNullable?.let { nonNullFeedItem ->
                                    openDialogRequiringReview(nonNullFeedItem)
                                }
                            },
                            feedViewModel = feedViewModel

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
                            selectedFeedItemForDialog = null
                            reviewForDialog = null
                        },
                        onEditClick = {
                            // 이 다이얼로그의 수정 버튼은 ReviewPreviewDialog 자체의 상태를 사용
                            val currentReview = reviewForDialog
                            val currentFeedItem = selectedFeedItemForDialog

                            showFeedItemDialog = false
                            selectedFeedItemForDialog = null
                            reviewForDialog = null

                            currentReview?.let { review ->
                                navController.navigate("review_edit/${review.id}")
                            }
                            Log.d("ReviewPreviewDialog", "Edit clicked for ReviewID ${currentReview?.id} in FeedID ${currentFeedItem?.id}")
                        },
                        onDeleteClick = {
                            // 이 다이얼로그의 삭제 버튼은 ReviewPreviewDialog 자체의 상태를 사용
                            val currentReview = reviewForDialog
                            val currentFeedItem = selectedFeedItemForDialog

                            showFeedItemDialog = false
                            selectedFeedItemForDialog = null
                            reviewForDialog = null

                            if (currentFeedItem != null && currentReview != null) {
                                viewModel.deleteReview(currentFeedItem.id, currentReview.id)
                                Log.d("ReviewPreviewDialog", "Delete clicked for ReviewID ${currentReview.id} in FeedID ${currentFeedItem.id}")
                                scope.launch {
                                    snackbarHostState.showSnackbar("리뷰가 삭제되었습니다.")
                                }
                            }
                        }
                    )
                } else if (showFeedItemDialog && selectedFeedItemForDialog != null && reviewForDialog == null) {
                    // 이 경우는 openDialogRequiringReview에서 Snackbar를 이미 표시하므로,
                    // 추가적인 LaunchedEffect는 중복될 수 있음.
                    // Dialog를 띄우지 않고 Snackbar만 표시하는 것이 openDialogRequiringReview의 역할임.
                    // 만약 그래도 Dialog를 띄우고 싶다면, openDialogRequiringReview 로직 수정 필요.
                    // 여기서는 showFeedItemDialog가 true로 설정되지 않도록 하는 것이 더 일관적일 수 있음.
                    // 현재는 showFeedItemDialog가 true로 설정되지 않으므로 이 LaunchedEffect는 실행되지 않을 가능성이 높음.
                    LaunchedEffect(selectedFeedItemForDialog) {
                        if(showFeedItemDialog) { // 실제로 이 조건이 true가 될 가능성은 낮음
                            scope.launch {
                                snackbarHostState.showSnackbar("${selectedFeedItemForDialog?.bookTitle}에는 표시할 리뷰가 없습니다. (Dialog LaunchedEffect)")
                            }
                            showFeedItemDialog = false
                            selectedFeedItemForDialog = null
                        }
                    }
                }
            }
        }
    }
}
