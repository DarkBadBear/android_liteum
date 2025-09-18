package com.peachspot.liteum.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.peachspot.liteum.R
import com.peachspot.liteum.data.db.BookLogs
import com.peachspot.liteum.viewmodel.BookSearchViewModel
import com.peachspot.liteum.viewmodel.BookSearchViewModelFactory
import com.peachspot.liteum.viewmodel.HomeViewModel
import com.peachspot.liteum.ui.components.DateInputTextField
import com.peachspot.liteum.ui.components.ImagePickerSection
import com.peachspot.liteum.ui.components.MyDatePickerDialog
import com.peachspot.liteum.ui.components.ReviewSectionCard
import com.peachspot.liteum.ui.components.SectionTitle
import com.peachspot.liteum.ui.components.StarRatingInput
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReviewEditScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    reviewId: Long, // BookLogs의 PrimaryKey 타입에 맞춰 Long으로 변경
    modifier: Modifier = Modifier,
) {
    // 기존 리뷰 데이터 로드
    val existingReview by viewModel.getBookLogById(reviewId).collectAsState(initial = null)

    // 상태 변수들 - BookLogs 엔티티 필드에 맞춰 초기화
    var bookTitle by remember { mutableStateOf("") }
    var reviewText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0f) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempImageFile by remember { mutableStateOf<File?>(null) }
    var author by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var publishDate by remember { mutableStateOf("") } // String 타입으로 변경
    var isbn by remember { mutableStateOf("") }
    var startReadDate by remember { mutableStateOf("") } // String 타입으로 변경
    var endReadDate by remember { mutableStateOf("") } // String 타입으로 변경
    var bookGenre by remember { mutableStateOf("") } // 장르 추가
    var pageCount by remember { mutableStateOf("") } // 페이지 수 추가

    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(reviewId) {
        // 타임아웃 설정 (예: 5초)
        kotlinx.coroutines.delay(5000)
        if (existingReview == null) {
            isLoading = false
            hasError = true
        }
    }

    LaunchedEffect(existingReview) {
        if (existingReview != null) {
            isLoading = false
            hasError = false
        }
    }
    // BookLogs 데이터로 상태 초기화
    LaunchedEffect(existingReview) {
        existingReview?.let { bookLog ->
            bookTitle = bookLog.bookTitle
            rating = bookLog.rating ?: 0f
            author = bookLog.author ?: ""
            publisher = bookLog.publisher ?: ""
            publishDate = bookLog.publishDate ?: ""
            isbn = bookLog.isbn ?: ""
            startReadDate = bookLog.startReadDate ?: ""
            endReadDate = bookLog.endReadDate ?: ""
            bookGenre = bookLog.bookGenre ?: ""
            pageCount = bookLog.pageCount?.toString() ?: ""
            selectedImageUri =
                bookLog.coverImageUri.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
        }
    }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var showPublishDatePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter =
        remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) } // BookLogs의 날짜 형식에 맞춤
    val bookSearchViewModelFactory = BookSearchViewModelFactory(viewModel)

    val bookSearchViewModel: BookSearchViewModel = viewModel(
        factory = bookSearchViewModelFactory
    )
    val isLoadingBooks by bookSearchViewModel.isLoading.collectAsState()
    val searchError by bookSearchViewModel.errorMessage.collectAsState()

    // 갤러리 실행을 위한 Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        tempImageFile = null
    }

    // 카메라 실행 및 결과 처리를 위한 Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                selectedImageUri = tempImageFile?.let { file ->
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }
            } else {
                tempImageFile?.delete()
                tempImageFile = null
            }
        }
    )

    val datePickerState = rememberDatePickerState()
    val confirmEnabled by remember {
        derivedStateOf { datePickerState.selectedDateMillis != null }
    }

    fun String?.toFormattedDateString(): String =
        this?.takeIf { it.isNotEmpty() }?.let { dateString ->
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
                SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN).format(date!!)
            } catch (e: Exception) {
                dateString // 파싱 실패 시 원본 반환
            }
        } ?: "선택 안 함"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "리뷰 수정", // 또는 stringResource(R.string.screen_title_review_edit)
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (bookTitle.isNotBlank() && reviewText.isNotBlank() && rating > 0) {
                        // BookLogs 업데이트 로직
                        val updatedBookLog = existingReview?.copy(
                            bookTitle = bookTitle,
                            rating = rating,
                            author = author.takeIf { it.isNotBlank() },
                            publisher = publisher.takeIf { it.isNotBlank() },
                            publishDate = publishDate.takeIf { it.isNotBlank() },
                            isbn = isbn.takeIf { it.isNotBlank() },
                            startReadDate = startReadDate.takeIf { it.isNotBlank() },
                            endReadDate = endReadDate.takeIf { it.isNotBlank() },
                            bookGenre = bookGenre.takeIf { it.isNotBlank() },
                            pageCount = pageCount.takeIf { it.isNotBlank() }?.toIntOrNull(),
                            coverImageUri = selectedImageUri?.toString() ?: ""
                        )
                        updatedBookLog?.let {
                            // TODO: viewModel.updateBookLog(it)
                        }
                        navController.popBackStack()
                    } else {
                        // TODO: 필수 항목 입력 안내
                    }
                },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                text = { Text("수정 완료", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        // 로딩 중일 때 표시
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            hasError -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("해당 리뷰를 찾을 수 없습니다")
                        Button(onClick = { navController.popBackStack() }) {
                            Text("돌아가기")
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .animateContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    ImagePickerSection(
                        selectedImageUri = selectedImageUri,
                        onGalleryClick = { galleryLauncher.launch("image/*") },
                        onCameraClick = {
                            if (cameraPermissionState.status.isGranted) {
                                val newFile = context.createImageFileForCamera()
                                tempImageFile = newFile
                                val uriForCamera = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    newFile
                                )
                                cameraLauncher.launch(uriForCamera)
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        onImageClearClick = {
                            selectedImageUri = null
                            tempImageFile?.delete()
                            tempImageFile = null
                        }
                    )

                    ReviewSectionCard {
                        SectionTitle(text = "리뷰")

                        val searchResults by bookSearchViewModel.searchResults.collectAsState()
                        var showSearchResultsDialog by remember { mutableStateOf(false) }

                        LaunchedEffect(searchResults) {
                            if (searchResults.isNotEmpty()) {
                                showSearchResultsDialog = true
                            }
                        }

                        val keyboardController = LocalSoftwareKeyboardController.current

                        OutlinedTextField(
                            value = bookTitle,
                            onValueChange = { newTitle ->
                                bookTitle = newTitle
                                if (newTitle.length > 2) {
                                    // 자동 검색 로직 (선택 사항)
                                }
                            },
                            label = { Text("제목 *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (bookTitle.isNotBlank()) {
                                        bookSearchViewModel.searchBooksByTitle(bookTitle)
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_book),
                                    contentDescription = "Book icon",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (bookTitle.isNotBlank()) {
                                            bookSearchViewModel.searchBooksByTitle(bookTitle)
                                            keyboardController?.hide()
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_search),
                                        contentDescription = "Search button"
                                    )
                                }
                            }
                        )

                        if (isLoadingBooks) {
                            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                        }

                        searchError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (showSearchResultsDialog) {
                            AlertDialog(
                                onDismissRequest = { showSearchResultsDialog = false },
                                title = { Text("검색 결과 선택") },
                                text = {
                                    LazyColumn {
                                        items(searchResults) { bookItem ->
                                            val volumeInfo = bookItem.volumeInfo
                                            val foundIsbn = bookSearchViewModel.getIsbn(bookItem)
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        bookTitle = volumeInfo?.title ?: ""
                                                        author =
                                                            volumeInfo?.authors?.joinToString(", ")
                                                                ?: ""
                                                        publisher = volumeInfo?.publisher ?: ""
                                                        isbn = foundIsbn ?: ""
                                                        // API에서 페이지 수도 가져올 수 있다면
                                                        pageCount =
                                                            volumeInfo?.pageCount?.toString() ?: ""
                                                        showSearchResultsDialog = false
                                                    }
                                                    .padding(vertical = 8.dp)
                                            ) {
                                                Text(
                                                    volumeInfo?.title ?: "제목 없음",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    volumeInfo?.authors?.joinToString(", ")
                                                        ?: "저자 정보 없음",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                foundIsbn?.let {
                                                    Text(
                                                        "ISBN: $it",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showSearchResultsDialog = false }) {
                                        Text("닫기")
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DateInputTextField(
                                label = "시작",
                                date = startReadDate.toFormattedDateString(),
                                onClick = { showStartDatePicker = true },
                                modifier = Modifier.weight(1f)
                            )
                            DateInputTextField(
                                label = "완료",
                                date = endReadDate.toFormattedDateString(),
                                onClick = { showEndDatePicker = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        StarRatingInput(
                            currentRating = rating,
                            onRatingChange = { rating = it },
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            label = { Text("리뷰 내용 *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 120.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            maxLines = 5
                        )
                    }

                    ReviewSectionCard {
                        SectionTitle(text = "도서 정보 (선택 입력)")

                        OutlinedTextField(
                            value = author,
                            onValueChange = { author = it },
                            label = { Text("저자") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = publisher,
                            onValueChange = { publisher = it },
                            label = { Text("출판사") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        DateInputTextField(
                            label = "출간일",
                            date = publishDate.toFormattedDateString(),
                            onClick = { showPublishDatePicker = true }
                        )

                        OutlinedTextField(
                            value = isbn,
                            onValueChange = { isbn = it },
                            label = { Text("ISBN (선택)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )

                        OutlinedTextField(
                            value = bookGenre,
                            onValueChange = { bookGenre = it },
                            label = { Text("장르 (선택)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = pageCount,
                            onValueChange = { pageCount = it },
                            label = { Text("페이지 수 (선택)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // 날짜 선택 다이얼로그들
        if (showPublishDatePicker) {
            MyDatePickerDialog(
                datePickerState = datePickerState,
                onDismiss = { showPublishDatePicker = false },
                onDateSelected = { selectedMillis ->
                    publishDate = selectedMillis?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                    } ?: ""
                    showPublishDatePicker = false
                },
                confirmEnabled = confirmEnabled
            )
        }

        if (showStartDatePicker) {
            MyDatePickerDialog(
                datePickerState = datePickerState,
                onDismiss = { showStartDatePicker = false },
                onDateSelected = { selectedMillis ->
                    startReadDate = selectedMillis?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                    } ?: ""
                    showStartDatePicker = false
                },
                confirmEnabled = confirmEnabled
            )
        }

        if (showEndDatePicker) {
            MyDatePickerDialog(
                datePickerState = datePickerState,
                onDismiss = { showEndDatePicker = false },
                onDateSelected = { selectedMillis ->
                    endReadDate = selectedMillis?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                    } ?: ""
                    showEndDatePicker = false
                },
                confirmEnabled = confirmEnabled
            )
        }
    }
}
