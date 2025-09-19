package com.peachspot.liteum.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
// import androidx.annotation.DrawableRes // 사용하지 않으면 제거 가능
import androidx.compose.animation.animateContentSize
// import androidx.compose.foundation.Image // Coil 사용 시 직접 사용 안 할 수 있음
// import androidx.compose.foundation.background // 사용하지 않으면 제거 가능
import androidx.compose.foundation.clickable
// import androidx.compose.foundation.interaction.MutableInteractionSource // 사용하지 않으면 제거 가능
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
// import androidx.compose.foundation.shape.CircleShape // 사용하지 않으면 제거 가능
// import androidx.compose.foundation.shape.RoundedCornerShape // 사용하지 않으면 제거 가능
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
// import androidx.compose.material.icons.filled.Delete // 사용하지 않으면 제거 가능

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
// import androidx.compose.runtime.getValue // 명시적 import 불필요
// import androidx.compose.runtime.setValue // 명시적 import 불필요
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.draw.clip // 사용하지 않으면 제거 가능
// import androidx.compose.ui.graphics.Color // 사용하지 않으면 제거 가능
// import androidx.compose.ui.layout.ContentScale // ImagePickerSection 내부에서 사용
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
// import androidx.compose.ui.tooling.preview.Preview // 프리뷰에서만 필요
// import androidx.compose.ui.unit.Dp // 사용하지 않으면 제거 가능
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// import coil.compose.rememberAsyncImagePainter // ImagePickerSection 내부에서 사용될 수 있음
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.peachspot.liteum.R // 실제 R 파일 경로로 확인 필요
import com.peachspot.liteum.data.repositiory.BookRepository
import com.peachspot.liteum.viewmodel.BookSearchViewModel
import com.peachspot.liteum.viewmodel.HomeViewModel // 실제 ViewModel 경로로 확인 필요
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
// import kotlin.collections.isNotEmpty // 명시적 import 불필요
// import kotlin.collections.map // 명시적 import 불필요

import com.peachspot.liteum.ui.components.DateInputTextField
import com.peachspot.liteum.ui.components.ImagePickerSection
import com.peachspot.liteum.ui.components.MyDatePickerDialog
import com.peachspot.liteum.ui.components.ReviewSectionCard
import com.peachspot.liteum.ui.components.SectionTitle
import com.peachspot.liteum.ui.components.StarRatingInput
import com.peachspot.liteum.util.createImageFileForCamera
import com.peachspot.liteum.util.saveImageToInternalStorageWithResizing

import com.peachspot.liteum.viewmodel.BookSearchViewModelFactory

// --- 이미지 저장 유틸리티 함수 ---
fun saveImageToInternalStorage(context: Context, uri: Uri, desiredFileNamePrefix: String = "LITEUM_COVER_"): File? {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null

    // 저장할 디렉토리 (예: /data/data/com.your.package/files/images)
    val outputDir = File(context.filesDir, "images")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }

    // 파일 이름 생성 (중복 방지를 위해 타임스탬프 사용)
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.getDefault()).format(Date())
    val fileExtension = ".jpg" // 기본 확장자, 실제 MIME 타입에 따라 변경 가능
    val fileName = "${desiredFileNamePrefix}${timeStamp}${fileExtension}"
    val outputFile = File(outputDir, fileName)

    try {
        FileOutputStream(outputFile).use { outputStream ->
            inputStream.use { input ->
                input.copyTo(outputStream)
            }
        }
        Log.d("ImageSaveUtil", "이미지 내부 저장 성공: ${outputFile.absolutePath}")
        return outputFile
    } catch (e: IOException) {
        Log.e("ImageSaveUtil", "이미지 저장 실패", e)
        outputFile.delete() // 실패 시 불완전한 파일 삭제
        return null
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReviewScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    bookRepository: BookRepository, // <<-- BookRepository를 파라미터로 직접 받음
    modifier: Modifier = Modifier,
) {
    var bookTitle by remember { mutableStateOf("") }
    var reviewText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0f) }

    // UI에 표시될 이미지 Uri (로컬 파일 Uri 또는 Content Uri가 될 수 있음)
    var displayImageUri by remember { mutableStateOf<Uri?>(null) }
    // 카메라 촬영 시 임시 파일 저장용
    var tempCameraImageFile by remember { mutableStateOf<File?>(null) }
    // DB에 저장될 최종 이미지 파일의 경로 (String)
    var finalImageFilePath by remember { mutableStateOf<String?>(null) }

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

    val bookSearchViewModelFactory = remember(viewModel, bookRepository) { // remember의 키로도 추가
        BookSearchViewModelFactory(viewModel, bookRepository)
    }
    val bookSearchViewModel: BookSearchViewModel = viewModel(factory = bookSearchViewModelFactory)


    val isLoadingBooks by bookSearchViewModel.isLoading.collectAsState()
    val searchError by bookSearchViewModel.errorMessage.collectAsState()


// ReviewScreen.kt 내의 galleryLauncher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            // <<--- 여기를 수정 --- >>
            // 리사이징 및 압축 기능이 있는 함수로 교체
            // targetWidth, targetHeight, quality는 원하는 값으로 설정
            val savedImageFile = saveImageToInternalStorageWithResizing(
                context = context,
                uri = sourceUri,
                desiredFileNamePrefix = "GALLERY_RESIZED_", // 파일명 접두사 변경 가능
                targetWidth = 1280, // 예시: 최대 너비 1280px
                targetHeight = 1280, // 예시: 최대 높이 1280px
                quality = 80 // 예시: JPEG 압축 품질 80%
            )

            if (savedImageFile != null) {
                displayImageUri = Uri.fromFile(savedImageFile)
                finalImageFilePath = savedImageFile.absolutePath
                Log.d("ReviewScreen", "갤러리 이미지 (리사이징 후) 저장: ${finalImageFilePath}")
            } else {
                Log.e("ReviewScreen", "갤러리 이미지 내부 저장/리사이징 실패")
                // 사용자에게 오류 메시지 표시 (예: Snackbar)
                // Toast.makeText(context, "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
            tempCameraImageFile = null
        }
    }

// ReviewScreen.kt 내의 cameraLauncher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempCameraImageFile?.let { capturedFile ->
                    val sourceUriForCamera = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        capturedFile
                    )
                    // <<--- 여기를 수정 --- >>
                    val savedImageFileFromCamera = saveImageToInternalStorageWithResizing(
                        context = context,
                        uri = sourceUriForCamera,
                        desiredFileNamePrefix = "CAMERA_RESIZED_",
                        targetWidth = 1280,
                        targetHeight = 1280,
                        quality = 80
                    )

                    if (savedImageFileFromCamera != null) {
                        displayImageUri = Uri.fromFile(savedImageFileFromCamera)
                        finalImageFilePath = savedImageFileFromCamera.absolutePath
                        Log.d("ReviewScreen", "카메라 이미지 (리사이징 후) 저장: ${finalImageFilePath}")
                        capturedFile.delete()
                        tempCameraImageFile = null
                    } else {
                        Log.e("ReviewScreen", "카메라 이미지 내부 저장/리사이징 실패")
                        // 사용자에게 오류 메시지 표시
                        // 임시 파일은 그대로 둘 수도 있지만, 결국 리사이징 실패한 것이므로 삭제 고려
                        capturedFile.delete() // 실패 시 임시 파일도 삭제
                        tempCameraImageFile = null
                    }
                }
            } else {
                tempCameraImageFile?.delete()
                tempCameraImageFile = null
                Log.d("ReviewScreen", "카메라 촬영 취소 또는 실패")
            }
        }
    )

    val datePickerState = rememberDatePickerState() // Material 3 DatePickerState
    val confirmEnabled by remember {
        derivedStateOf { datePickerState.selectedDateMillis != null }
    }

    fun Date?.toFormattedString(): String = this?.let { dateFormatter.format(it) } ?: "선택 안 함"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.screen_title_review_creation),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_up)
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
                    // finalImageFilePath가 null이 아니어야 이미지가 정상적으로 저장된 것
                    if (bookTitle.isNotBlank() && finalImageFilePath != null ) {
                        val currentMemberId = "temp_user_id" // TODO: 실제 사용자 ID 가져오기
                        val sharePreference = "Y" // TODO: UI에서 사용자 선택 받기 (공개/비공개)

                        viewModel.saveBookAndReview(
                            bookTitle = bookTitle,
                            selectedImageFilePath = finalImageFilePath, // 저장된 파일 경로 전달
                            reviewText = reviewText,
                            rating = rating,
                            author = author.ifBlank { null },
                            publisher = publisher.ifBlank { null },
                            publishDate = publishDate,
                            isbn = isbn.ifBlank { null },
                            startDate = startDate,
                            endDate = endDate,
                            memberId = currentMemberId,
                            shareReview = sharePreference
                        )
                        Log.d("ReviewScreen", "리뷰 저장 요청: Book '$bookTitle', ImagePath '$finalImageFilePath'")
                        navController.popBackStack()
                        // TODO: 저장 성공 Snackbar 표시
                    } else {
                        Log.w("ReviewScreen", "필수 항목 누락: 제목, 이미지, 리뷰, 별점 중 하나 이상이 비어있거나 이미지 저장이 안됨.")
                        // TODO: 사용자에게 필수 항목 입력 안내 (Snackbar 등)
                        // 예: Toast.makeText(context, "제목, 이미지, 리뷰 내용, 별점은 필수 항목입니다.", Toast.LENGTH_LONG).show()
                    }
                },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                text = { Text(stringResource(R.string.button_save_review), fontWeight = FontWeight.SemiBold) },
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
                selectedImageUri = displayImageUri, // UI 표시용 Uri
                onGalleryClick = {
                    galleryLauncher.launch("image/*")
                                 },
                onCameraClick = {
                    if (cameraPermissionState.status.isGranted) {
                        val newFile = context.createImageFileForCamera()
                        tempCameraImageFile = newFile
                        val uriForCamera = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider", // AndroidManifest.xml의 authorities와 일치
                            newFile
                        )
                        cameraLauncher.launch(uriForCamera)
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                onImageClearClick = {
                    displayImageUri = null
                    finalImageFilePath = null // DB에 저장될 경로도 초기화
                    // 만약 tempCameraImageFile이 있다면 그것도 삭제/초기화 고려
                    tempCameraImageFile?.delete()
                    tempCameraImageFile = null
                    Log.d("ReviewScreen", "이미지 선택 초기화")
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
                    onValueChange = { newTitle -> bookTitle = newTitle },
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

                if (showSearchResultsDialog && searchResults.isNotEmpty()) {
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
                                                author = volumeInfo?.authors?.joinToString(", ") ?: ""
                                                publisher = volumeInfo?.publisher ?: ""
                                                isbn = foundIsbn ?: ""
                                                // TODO: API에서 출간일 정보 파싱하여 publishDate 설정
                                                showSearchResultsDialog = false
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateInputTextField(
                        label = "시작",
                        date = startDate.toFormattedString(),
                        onClick = { showStartDatePicker = true },
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
                    label = { Text("리뷰 내용") },
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
