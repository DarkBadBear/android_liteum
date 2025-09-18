package com.peachspot.liteum.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material3.*
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.peachspot.liteum.R // 실제 R 파일 경로로 확인 필요
import com.peachspot.liteum.viewmodel.BookSearchViewModel
import com.peachspot.liteum.viewmodel.HomeViewModel // 실제 ViewModel 경로로 확인 필요
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.isNotEmpty
import kotlin.collections.map

import androidx.compose.runtime.Composable

import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.dp
import com.peachspot.liteum.ui.components.DateInputTextField
import com.peachspot.liteum.ui.components.ImagePickerSection
import com.peachspot.liteum.ui.components.MyDatePickerDialog
import com.peachspot.liteum.ui.components.ReviewSectionCard
import com.peachspot.liteum.ui.components.SectionTitle
import com.peachspot.liteum.ui.components.StarRatingInput
import com.peachspot.liteum.viewmodel.BookSearchViewModelFactory

// Context 확장 함수를 만들어 파일 생성 및 Uri 가져오기를 쉽게 합니다.
fun Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = File(externalCacheDir, "images") // AndroidManifest.xml 및 file_paths.xml 와 일치 확인
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }
    return File.createTempFile(
        imageFileName, /* prefix */
        ".jpg",       /* suffix */
        storageDir    /* directory */
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    viewModel: HomeViewModel, // 사용하지 않는다면 제거 또는 ReviewViewModel 등으로 대체
    modifier: Modifier = Modifier,
) {
    var bookTitle by remember { mutableStateOf("") }
    var reviewText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0f) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // 최종 선택/촬영된 이미지 Uri
    var tempImageFile by remember { mutableStateOf<File?>(null) } // 카메라 촬영 시 임시 파일 저장용

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var author by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var publishDate by remember { mutableStateOf<Date?>(null) }
    var isbn by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    var showPublishDatePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN) }
    val bookSearchViewModelFactory = BookSearchViewModelFactory(viewModel)
    Log.d("ViewModelDebug", "BookSearchViewModelFactory instance: $bookSearchViewModelFactory") // 로그 추가

    val bookSearchViewModel: BookSearchViewModel = viewModel(
        factory = bookSearchViewModelFactory
    )
    Log.d("ViewModelDebug", "BookSearchViewModel instance: $bookSearchViewModel") // 로그 추가
    val isLoadingBooks by bookSearchViewModel.isLoading.collectAsState()
    val searchError by bookSearchViewModel.errorMessage.collectAsState()


    // 갤러리 실행을 위한 Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        tempImageFile = null // 갤러리 선택 시 임시 파일은 사용 안 함
    }

    // 카메라 실행 및 결과 처리를 위한 Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                selectedImageUri = tempImageFile?.let { file ->
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider", // AndroidManifest.xml의 authorities와 일치
                        file
                    )
                }
            } else {
                // 촬영 실패 또는 취소 시 임시 파일 삭제
                tempImageFile?.delete()
                tempImageFile = null
            }
        }
    )

    val datePickerState = rememberDatePickerState()
    val confirmEnabled by remember {
        derivedStateOf { datePickerState.selectedDateMillis != null }
    }

    fun Date?.toFormattedString(): String = this?.let { dateFormatter.format(it) } ?: "선택 안 함"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.screen_title_review_creation), // 문자열 리소스 확인
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_up) // 문자열 리소스 확인
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
                    if (bookTitle.isNotBlank() && selectedImageUri != null && reviewText.isNotBlank() && rating > 0) {
                        // TODO: 저장 로직 (viewModel.saveReview(...))
                        // 예: viewModel.saveReview(bookTitle, reviewText, rating, selectedImageUri, author, ...)
                        navController.popBackStack()
                    } else {
                        // TODO: 사용자에게 필수 항목 입력 안내 (Snackbar 등)
                    }
                },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                text = { Text(stringResource(R.string.button_save_review), fontWeight = FontWeight.SemiBold) }, // 문자열 리소스 확인
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
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
                        val newFile = context.createImageFile()
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
                SectionTitle(
                    text = "리뷰",
                    //iconResId = R.drawable.ic_book // 아이콘 리소스 확인
                )

                val searchResults by bookSearchViewModel.searchResults.collectAsState()

                var showSearchResultsDialog by remember { mutableStateOf(false) }

                // searchResults가 비어있지 않고, 아직 다이얼로그가 표시되지 않았다면 표시하도록 설정
                // 사용자가 명시적으로 닫기 전까지는 계속 떠 있도록 하거나, 새로운 검색 시 자동으로 갱신/표시되도록 할 수 있음
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
                        // 사용자가 입력을 멈췄을 때 자동으로 검색 (Debounce 적용 추천)
                        if (newTitle.length > 2) { // 예: 3글자 이상 입력 시 검색
                            // 자동 검색 로직 (선택 사항)
                            // bookSearchViewModel.searchBooksByTitle(newTitle)
                        }
                    },
                    label = { Text("제목 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search // 키보드 액션 버튼을 '검색'으로 설정
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { // 키보드의 '검색' 버튼 클릭 시
                            if (bookTitle.isNotBlank()) {
                                bookSearchViewModel.searchBooksByTitle(bookTitle)
                                keyboardController?.hide() // 키보드 숨기기
                            }
                            // showSearchResultsDialog = true (결과가 있을 때)
                        }
                    ),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_book), // 기존 아이콘 리소스
                            contentDescription = "Book icon", // contentDescription 추가 권장
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = { // 오른쪽 끝에 아이콘 추가
                        IconButton(
                            onClick = {
                                // 돋보기 아이콘 클릭 시 검색 실행
                                if (bookTitle.isNotBlank()) {
                                    bookSearchViewModel.searchBooksByTitle(bookTitle)
                                    keyboardController?.hide() // 키보드 숨기기
                                }
                                // showSearchResultsDialog = true (결과가 있을 때)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search), // 기존 아이콘 리소스
                                contentDescription = "Search button" // 접근성을 위한 설명
                            )
                        }
                    }
                )



// 검색 로딩 상태 표시 (예시)
                if (isLoadingBooks) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                }

// 검색 오류 메시지 표시 (예시)
                searchError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (searchResults.isNotEmpty()) { // 또는 showSearchResultsDialog 상태 사용
                    // 예시: 간단히 첫 번째 결과의 제목을 로그로 출력
                    // 실제로는 AlertDialog, DropdownMenu 등을 사용하여 사용자에게 선택지를 제공
                    Log.d("BookSearch", "검색 결과: ${searchResults.map { it.volumeInfo?.title }}")

                    // AlertDialog 예시 (간단하게)
                    // LaunchedEffect(searchResults) { showSearchResultsDialog = true } // 검색 결과 있을 때 다이얼로그 표시
                    if (showSearchResultsDialog) { // 이 상태를 관리하는 로직 필요
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
                                                    author = volumeInfo?.authors?.joinToString(", ")
                                                        ?: ""
                                                    publisher = volumeInfo?.publisher ?: ""
                                                    // publishDate, isbn 등도 유사하게 설정
                                                    // 예: isbn = foundIsbn ?: ""
                                                    showSearchResultsDialog = false // 선택 후 다이얼로그 닫기
                                                }
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text(volumeInfo?.title ?: "제목 없음", style = MaterialTheme.typography.titleMedium)
                                            Text(volumeInfo?.authors?.joinToString(", ") ?: "저자 정보 없음", style = MaterialTheme.typography.bodyMedium)
                                            foundIsbn?.let { Text("ISBN: $it", style = MaterialTheme.typography.bodySmall) }
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
                }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DateInputTextField(
                            label = "시작",
                            date = startDate.toFormattedString(),
                            onClick = {
                                Log.d("DatePickerDebug", "시작 날짜 DateInputTextField 클릭됨!") // <--- 이 로그 확인!
                                showStartDatePicker = true
                                Log.d("DatePickerDebug", "showStartDatePicker 상태 변경 후: $showStartDatePicker") // <--- 이 로그 확인!

                            },
                            modifier = Modifier.weight(1f)
                        )
                        DateInputTextField(
                            label = "완료",
                            date = endDate.toFormattedString(),
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
                    date = publishDate.toFormattedString(),
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
                        imeAction = ImeAction.Done
                    )
                )
            }

            Spacer(modifier = Modifier.height(80.dp)) // FAB를 위한 공간 확보
        }
    }

    if (showPublishDatePicker) {
        MyDatePickerDialog(
            datePickerState = datePickerState,
            onDismiss = { showPublishDatePicker = false },
            onDateSelected = { selectedMillis ->
                publishDate = selectedMillis?.let { Date(it) }
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
                startDate = selectedMillis?.let { Date(it) }
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
                endDate = selectedMillis?.let { Date(it) }
                showEndDatePicker = false
            },
            confirmEnabled = confirmEnabled
        )
    }
}

