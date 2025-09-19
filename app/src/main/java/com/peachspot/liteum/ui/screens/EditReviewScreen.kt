package com.peachspot.liteum.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.copy
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.peachspot.liteum.R
import com.peachspot.liteum.data.db.BookLogs // 가정: ViewModel에서 사용할 데이터 클래스
import com.peachspot.liteum.data.repositiory.BookRepository
import com.peachspot.liteum.viewmodel.BookSearchViewModel
import com.peachspot.liteum.viewmodel.BookSearchViewModelFactory
import com.peachspot.liteum.viewmodel.HomeViewModel // 가정: AuthUiState 포함
import com.peachspot.liteum.ui.components.DateInputTextField
import com.peachspot.liteum.ui.components.ImagePickerSection
import com.peachspot.liteum.ui.components.MyDatePickerDialog
import com.peachspot.liteum.ui.components.ReviewSectionCard
import com.peachspot.liteum.ui.components.SectionTitle
import com.peachspot.liteum.ui.components.StarRatingInput
import com.peachspot.liteum.util.createImageFileForCamera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

// ViewModel에서 BookLog와 ReviewLog 정보를 합쳐서 UI에 제공하기 위한 데이터 클래스 (예시)
// 실제로는 ViewModel 또는 data 모듈에 위치하는 것이 좋음
data class EditableBookReview(
    val bookLog: BookLogs,
    val reviewText: String?,
    val shareSetting: String? // 예: "private", "public"
)

// Context 확장 함수 - 이미지 URI를 내부 저장소로 복사하고 새 파일 URI 반환 (구현 필요)
fun Context.copyUriToInternalStorage(uri: Uri, fileNamePrefix: String = "img_review_"): Uri? {
    return try {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val extension = when (contentResolver.getType(uri)) {
            "image/jpeg" -> ".jpg"
            "image/png" -> ".png"
            else -> "" // 지원하지 않는 타입이거나 알 수 없는 경우
        }
        val fileName = "${fileNamePrefix}${System.currentTimeMillis()}$extension"
        val file = File(filesDir, fileName) // 앱 내부 files 디렉토리

        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
    } catch (e: Exception) {
        Log.e("ImageUtil", "Error copying URI to internal storage", e)
        null
    }
}



@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReviewEditScreen(
    navController: NavController,
    viewModel: HomeViewModel, // HomeViewModel 주입
    bookLogId: Long, // BookLogs의 ID
    bookRepository: BookRepository, // <<-- BookRepository를 파라미터로 직접 받음
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ViewModel의 UI 상태 관찰
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 수정할 책+리뷰 데이터 로드 (ViewModel에 getEditableBookReview 함수 필요)
    // 이 Flow는 BookLogs와 연결된 ReviewLogs 정보를 합쳐서 EditableBookReview 객체로 반환한다고 가정
    val editableReviewDataFlow = remember(bookLogId, viewModel) {
        viewModel.getEditableBookReview(bookLogId) // ViewModel에 해당 함수 구현 필요
    }
    val editableReviewState by editableReviewDataFlow.collectAsStateWithLifecycle(initialValue = null)

    // UI 상태 변수들
    var bookTitle by remember { mutableStateOf("") }
    var currentReviewText by remember { mutableStateOf("") } // reviewText -> currentReviewText로 변경 (상태 혼동 방지)
    var rating by remember { mutableStateOf(0f) }
    var internalImageUri by remember { mutableStateOf<Uri?>(null) } // DB에 저장될 최종 이미지 URI (내부 저장소 경로)
    var displayImageUri by remember { mutableStateOf<Uri?>(null) } // 화면 표시에 사용될 URI (초기에는 DB값, 변경 시 새 선택 URI)
    var tempCameraFile by remember { mutableStateOf<File?>(null) } // 카메라 촬영용 임시 파일

    var author by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") }
    var publishDateString by remember { mutableStateOf("") }
    var isbn by remember { mutableStateOf("") }
    var startReadDateString by remember { mutableStateOf("") }
    var endReadDateString by remember { mutableStateOf("") }
    var bookGenre by remember { mutableStateOf("") }
    var pageCount by remember { mutableStateOf("") }
    var currentShareSetting by remember { mutableStateOf("private") } // 리뷰 공유 설정

    var initialLoading by remember { mutableStateOf(true) }
    var dataError by remember { mutableStateOf<String?>(null) }

    // 데이터 로드 및 상태 초기화
// ReviewEditScreen.kt

    // 데이터 로드 및 상태 초기화
    LaunchedEffect(editableReviewState) {
        if (editableReviewState != null) {
            // editableReviewState에 유효한 데이터가 있는 경우
            val data = editableReviewState!! // Non-null로 캐스팅 (위에서 null 체크했으므로 안전)

            initialLoading = false
            dataError = null // 이전 데이터 오류가 있었다면 초기화

            // BookLog 데이터는 필수라고 가정 (화면 진입 시 유효한 bookLogId를 받았으므로)
            // 만약 viewModel.getEditableBookReview에서 bookLog 조차 못 찾는 경우가 있다면,
            // editableReviewState 자체가 null이거나, EditableBookReview 내의 bookLog가 null일 수 있음.
            // 여기서는 editableReviewState.bookLog는 항상 유효하다고 가정하고 진행.
            // (ViewModel에서 bookLog를 못 찾으면 editableReviewState를 null로 emit해야 함)
            data.bookLog.let { bookLog ->
                bookTitle = bookLog.bookTitle
                author = bookLog.author ?: ""
                publisher = bookLog.publisher ?: ""
                publishDateString = bookLog.publishDate ?: ""
                isbn = bookLog.isbn ?: ""
                startReadDateString = bookLog.startReadDate ?: ""
                endReadDateString = bookLog.endReadDate ?: ""
                bookGenre = bookLog.bookGenre ?: ""
                pageCount = bookLog.pageCount?.toString() ?: ""

                // 표지 이미지 URI 처리
                if (!bookLog.coverImageUri.isNullOrEmpty()) {
                    try {
                        val parsedUri = Uri.parse(bookLog.coverImageUri)
                        internalImageUri = parsedUri
                        displayImageUri = parsedUri
                    } catch (e: Exception) {
                        Log.e("ReviewEditScreen", "Failed to parse coverImageUri: ${bookLog.coverImageUri}", e)
                        // 파싱 실패 시 기본 이미지 또는 null 처리
                        internalImageUri = null
                        displayImageUri = null
                    }
                } else {
                    internalImageUri = null
                    displayImageUri = null
                }
            }

            // 리뷰 관련 정보 초기화 (새 리뷰 작성 시에는 reviewText 등이 null일 수 있음)
            currentReviewText = data.reviewText ?: ""
            rating = data.bookLog.rating ?: 0f // rating은 BookLogs에 있을 수도 있고, 새 리뷰 작성 시 0f로 시작
            // EditableBookReview에 별도의 rating 필드가 있다면 그것을 사용
            // 여기서는 BookLogs의 rating을 기본값으로 사용하고, 없으면 0f
            currentShareSetting = data.shareSetting ?: "private"

        } else {
            // editableReviewState가 null인 경우 (데이터 로딩 중이거나, 로드 실패)
            // 초기 로딩 상태가 아직 true라면 계속 로딩 중으로 간주.
            // initialLoading이 false인데도 여기가 실행되면 데이터 로드 실패로 볼 수 있음.
            if (!initialLoading) { // 이미 초기 로딩 시도가 끝났는데 데이터가 null이면 오류로 간주
                // dataError를 설정하는 로직은 LaunchedEffect(bookLogId)의 타임아웃 처리와 중복될 수 있으므로,
                // ViewModel에서 로딩 상태와 오류 상태를 명확히 관리하고 UI에 반영하는 것이 더 좋음.
                // 여기서는 단순히 로그만 남기거나, ViewModel의 상태를 신뢰하고 추가 작업 안 함.
                Log.d("ReviewEditScreen", "editableReviewState is null after initial loading period.")
                // dataError = "리뷰 정보를 불러올 수 없습니다. (editableReviewState is null)" // 필요시
            }
        }
    }



    // 초기 데이터 로드 타임아웃 또는 실패 처리
    LaunchedEffect(bookLogId) {
        // editableReviewState가 일정 시간 후에도 null이면 오류로 간주 (ViewModel에서 이미 로딩 상태 관리 시 불필요할 수 있음)
        kotlinx.coroutines.delay(7000) // 예: 7초
        if (editableReviewState == null && initialLoading) {
            initialLoading = false
            dataError = "데이터를 불러오는 데 실패했습니다."
            Log.e("ReviewEditScreen", "Timeout or failure loading editable review data for bookLogId: $bookLogId")
        }
    }


    // ViewModel의 메시지 처리 (Toast 등)
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            Toast.makeText(context, "${uiState.userMessageType}: $message", Toast.LENGTH_LONG).show()
            viewModel.clearUserMessage() // 메시지 표시 후 초기화
            if (uiState.userMessageType == "success" && (message.contains("수정") || message.contains("저장"))) {
                navController.popBackStack()
            }
        }
    }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            displayImageUri = sourceUri // 화면에는 바로 보여줌
            // 선택된 이미지를 내부 저장소로 복사 (백그라운드에서)
            coroutineScope.launch(Dispatchers.IO) {
                val copiedUri = context.copyUriToInternalStorage(sourceUri)
                withContext(Dispatchers.Main) {
                    if (copiedUri != null) {
                        internalImageUri = copiedUri // DB에 저장될 URI 업데이트
                        Log.d("ReviewEditScreen", "Image copied to internal storage: $copiedUri")
                    } else {
                        displayImageUri = internalImageUri // 복사 실패 시 이전 이미지로 되돌림 (또는 사용자에게 알림)
                        Toast.makeText(context, "이미지 처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            tempCameraFile = null // 갤러리 선택 시 카메라 임시 파일은 무관
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.let { file ->
                val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                displayImageUri = fileUri // 화면에는 바로 보여줌 (내부 저장소 복사 전)
                // 촬영된 이미지를 내부 저장소의 최종 위치로 복사 (선택적: tempCameraFile이 이미 앱별 저장소면 그대로 사용 가능)
                coroutineScope.launch(Dispatchers.IO) {
                    // copyUriToInternalStorage는 content URI를 주로 다루므로, File URI를 직접 사용하거나
                    // tempCameraFile을 원하는 최종 위치로 이동/복사하는 로직이 필요할 수 있음.
                    // 여기서는 FileProvider URI를 internalImageUri로 우선 설정.
                    // 만약 copyUriToInternalStorage가 File URI도 잘 처리한다면 그것을 사용.
                    // 간단히 File URI 자체를 internalImageUri로 사용한다고 가정.
                    val finalImageUri = FileProvider.getUriForFile(context,"${context.packageName}.fileprovider", file)
                    withContext(Dispatchers.Main) {
                        internalImageUri = finalImageUri
                        Log.d("ReviewEditScreen", "Image taken with camera: $finalImageUri")
                    }
                }
            }
        } else {
            tempCameraFile?.delete() // 촬영 실패 또는 취소 시 임시 파일 삭제
        }
        // tempCameraFile은 촬영 후에는 더 이상 직접적으로 사용되지 않으므로 null로 설정해도 무방
        // tempCameraFile = null
    }


    var showPublishDatePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val publishDatePickerState = rememberDatePickerState()
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    fun String?.toFormattedDateString(): String =
        this?.takeIf { it.isNotEmpty() }?.let { dateString ->
            try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
                SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN).format(date!!)
            } catch (e: Exception) {
                dateString
            }
        } ?: "선택 안 함"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("리뷰 수정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
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
                    if (uiState.isLoading) return@ExtendedFloatingActionButton // 중복 클릭 방지

                    if (bookTitle.isNotBlank() && currentReviewText.isNotBlank() && rating > 0) {
                        editableReviewState?.bookLog?.let { currentBookLog ->
                            val bookLogDataToUpdate = currentBookLog.copy(
                                bookTitle = bookTitle,
                                rating = rating,
                                author = author.takeIf { it.isNotBlank() },
                                publisher = publisher.takeIf { it.isNotBlank() },
                                publishDate = publishDateString.takeIf { it.isNotBlank() },
                                isbn = isbn.takeIf { it.isNotBlank() },
                                startReadDate = startReadDateString.takeIf { it.isNotBlank() },
                                endReadDate = endReadDateString.takeIf { it.isNotBlank() },
                                bookGenre = bookGenre.takeIf { it.isNotBlank() },
                                pageCount = pageCount.takeIf { it.isNotBlank() }?.toIntOrNull(),
                                coverImageUri = internalImageUri?.toString() ?: "" // 내부 저장소에 복사된 이미지 URI
                            )

                            viewModel.updateBookLogAndReview(
                                updatedBookLog = bookLogDataToUpdate,
                                newReviewText = currentReviewText,
                                newShareSetting = currentShareSetting
                            )
                        } ?: run {
                            Toast.makeText(context, "기존 독서 기록 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "제목, 별점, 리뷰 내용은 필수입니다.", Toast.LENGTH_SHORT).show()
                    }
                },
                icon = { if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Icon(Icons.Filled.Check, contentDescription = null) },
                text = { Text(if (uiState.isLoading) "수정 중..." else "수정 완료", fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                expanded = !uiState.isLoading // 로딩 중에는 아이콘만 보이도록 (선택적)
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        when {
            initialLoading && editableReviewState == null && dataError == null -> { // ViewModel의 uiState.isLoading으로 대체 가능
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("리뷰 정보를 불러오는 중...", modifier = Modifier.padding(top = 60.dp))
                }
            }
            dataError != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dataError ?: "데이터를 불러올 수 없습니다.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("돌아가기") }
                    }
                }
            }
            editableReviewState != null -> {
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
                        selectedImageUri = displayImageUri, // 화면 표시용 URI 사용
                        onGalleryClick = { galleryLauncher.launch("image/*") },
                        onCameraClick = {
                            if (cameraPermissionState.status.isGranted) {
                                val newFile = context.createImageFileForCamera()
                                tempCameraFile = newFile
                                val uriForCamera = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider", // AndroidManifest.xml의 provider authorities와 일치해야 함
                                    newFile
                                )
                                cameraLauncher.launch(uriForCamera)
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        onImageClearClick = {
                            displayImageUri = null
                            internalImageUri = null // DB에 저장될 URI도 초기화
                            tempCameraFile?.delete()
                            tempCameraFile = null
                        }
                    )

                    ReviewSectionCard {
                        SectionTitle(text = "리뷰 *")
                        // ... (OutlinedTextField for bookTitle - BookSearchViewModel 관련 코드는 생략하지 않고 유지)
                        val bookSearchViewModelFactory = remember(viewModel, bookRepository) { // viewModel과 bookRepository를 키로 사용
                            BookSearchViewModelFactory(viewModel, bookRepository) // 직접 받은 bookRepository 사용
                        }
                        val bookSearchViewModel: BookSearchViewModel = viewModel(factory = bookSearchViewModelFactory)
                        val searchResults by bookSearchViewModel.searchResults.collectAsStateWithLifecycle()
                        var showSearchResultsDialog by remember { mutableStateOf(false) }
                        val keyboardController = LocalSoftwareKeyboardController.current
                        val isLoadingBooks by bookSearchViewModel.isLoading.collectAsStateWithLifecycle()
                        val searchError by bookSearchViewModel.errorMessage.collectAsStateWithLifecycle()


                        LaunchedEffect(searchResults) {
                            if (searchResults.isNotEmpty()) {
                                showSearchResultsDialog = true
                            }
                        }
                        OutlinedTextField(
                            value = bookTitle,
                            onValueChange = { newTitle -> bookTitle = newTitle },
                            label = { Text("제목 *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (bookTitle.isNotBlank()) {
                                    bookSearchViewModel.searchBooksByTitle(bookTitle); keyboardController?.hide()
                                }
                            }),
                            leadingIcon = { Icon(painter = painterResource(R.drawable.ic_book), contentDescription = "Book icon", modifier = Modifier.size(24.dp)) },
                            trailingIcon = { IconButton(onClick = { if (bookTitle.isNotBlank()) { bookSearchViewModel.searchBooksByTitle(bookTitle); keyboardController?.hide() } }) { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = "Search button") } }
                        )
                        if (isLoadingBooks) CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                        searchError?.let { Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

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
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    bookTitle = volumeInfo?.title ?: ""
                                                    author = volumeInfo?.authors?.joinToString(", ") ?: ""
                                                    publisher = volumeInfo?.publisher ?: ""
                                                    isbn = foundIsbn ?: ""
                                                    pageCount = volumeInfo?.pageCount?.toString() ?: ""
                                                    // 출판일도 API에서 가져올 수 있다면
                                                    // publishDateString = volumeInfo?.publishedDate ?: "" // 형식 변환 필요할 수 있음
                                                    showSearchResultsDialog = false
                                                }.padding(vertical = 8.dp)
                                            ) {
                                                Text(volumeInfo?.title ?: "제목 없음", style = MaterialTheme.typography.titleMedium)
                                                Text(volumeInfo?.authors?.joinToString(", ") ?: "저자 정보 없음", style = MaterialTheme.typography.bodyMedium)
                                                foundIsbn?.let { Text("ISBN: $it", style = MaterialTheme.typography.bodySmall) }
                                            }
                                        }
                                    }
                                },
                                confirmButton = { TextButton(onClick = { showSearchResultsDialog = false }) { Text("닫기") } }
                            )
                        }


                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DateInputTextField(label = "시작일", date = startReadDateString.toFormattedDateString(), onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f))
                            DateInputTextField(label = "완료일", date = endReadDateString.toFormattedDateString(), onClick = { showEndDatePicker = true }, modifier = Modifier.weight(1f))
                        }
                        StarRatingInput(currentRating = rating, onRatingChange = { rating = it }, modifier = Modifier.padding(top = 8.dp))
                        OutlinedTextField(
                            value = currentReviewText,
                            onValueChange = { currentReviewText = it },
                            label = { Text("리뷰 내용 *") },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 120.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                            maxLines = 5
                        )
                        // TODO: 리뷰 공유 설정 UI (예: RadioButton, Dropdown)
                        // 예시로 간단한 Text 표시
                        Text("공유 설정: $currentShareSetting (UI 필요)", modifier = Modifier.padding(top = 8.dp))

                    }

                    ReviewSectionCard {
                        SectionTitle(text = "도서 정보 (선택 입력)")
                        OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("저자") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                        OutlinedTextField(value = publisher, onValueChange = { publisher = it }, label = { Text("출판사") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                        DateInputTextField(label = "출간일", date = publishDateString.toFormattedDateString(), onClick = { showPublishDatePicker = true })
                        OutlinedTextField(value = isbn, onValueChange = { isbn = it }, label = { Text("ISBN") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
                        OutlinedTextField(value = bookGenre, onValueChange = { bookGenre = it }, label = { Text("장르") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                        OutlinedTextField(value = pageCount, onValueChange = { pageCount = it }, label = { Text("페이지 수") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                    }
                    Spacer(modifier = Modifier.height(80.dp)) // FAB 공간 확보
                }
            }
            // 이 외의 경우는 editableReviewState가 null이고 error도 null인 초기 로딩 전 상태이거나, 예외적인 상황.
            // 필요하다면 추가적인 상태 처리
        }

        if (showPublishDatePicker) {
            MyDatePickerDialog(
                datePickerState = publishDatePickerState,
                onDismiss = { showPublishDatePicker = false },
                onDateSelected = { selectedMillis ->
                    publishDateString = selectedMillis?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: ""
                    showPublishDatePicker = false
                },
                confirmEnabled = publishDatePickerState.selectedDateMillis != null
            )
        }
        if (showStartDatePicker) {
            MyDatePickerDialog(
                datePickerState = startDatePickerState,
                onDismiss = { showStartDatePicker = false },
                onDateSelected = { selectedMillis ->
                    startReadDateString = selectedMillis?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: ""
                    showStartDatePicker = false
                },
                confirmEnabled = startDatePickerState.selectedDateMillis != null
            )
        }
        if (showEndDatePicker) {
            MyDatePickerDialog(
                datePickerState = endDatePickerState,
                onDismiss = { showEndDatePicker = false },
                onDateSelected = { selectedMillis ->
                    endReadDateString = selectedMillis?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) } ?: ""
                    showEndDatePicker = false
                },
                confirmEnabled = endDatePickerState.selectedDateMillis != null
            )
        }
    }
}
