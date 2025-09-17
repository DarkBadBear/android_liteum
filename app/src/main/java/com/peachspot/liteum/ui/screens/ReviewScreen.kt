package com.peachspot.liteum.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.peachspot.liteum.R // 실제 R 파일 경로로
import com.peachspot.liteum.ui.components.SectionTitle
import com.peachspot.liteum.viewmodel.HomeViewModel // 실제 ViewModel 경로로
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: HomeViewModel, // 사용하지 않는다면 제거 또는 ReviewViewModel 등으로 대체
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    var bookTitle by remember { mutableStateOf("") }
    var reviewText by remember { mutableStateOf("") } // 리뷰 내용 추가
    var rating by remember { mutableStateOf(0f) } // 별점 추가
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var author by remember { mutableStateOf("") }
    var publisher by remember { mutableStateOf("") } // 출판사 추가
    var publishDate by remember { mutableStateOf<Date?>(null) } // Date 타입으로 변경
    var isbn by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    // var bookType by remember { mutableStateOf("") } // 필요시 유지

    var showPublishDatePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // DatePicker 상태를 관리할 객체
    // ReviewScreen.kt 내의 DatePicker 관련 부분

    // DatePicker 상태를 관리할 객체
    val datePickerState = rememberDatePickerState()
    // remember로 derivedStateOf의 결과를 감싸줍니다.
    val confirmEnabled by remember { // by 키워드로 State<Boolean>에서 Boolean으로 바로 사용
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
                            contentDescription = stringResource(R.string.cd_navigate_up) // content description 추가
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
                        navController.popBackStack()
                    } else {
                        // TODO: 사용자에게 필수 항목 입력 안내 (Snackbar 등)
                    }
                },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                text = { Text(stringResource(R.string.button_save_review), fontWeight = FontWeight.SemiBold) },
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues) // Scaffold로부터의 패딩 적용
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp) // 화면 전체 좌우, 상하 패딩
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // 요소 간 기본 간격
        ) {

            // --- 책 표지 이미지 ---
            ImagePickerSection(
                selectedImageUri = selectedImageUri,
                onImagePickerClick = { imagePickerLauncher.launch("image/*") },
                onImageClearClick = { selectedImageUri = null }
            )

            // --- 필수 정보 섹션 ---
            ReviewSectionCard {
                SectionTitle(
                    text = "책과 나의 이야기",
                    iconResId = R.drawable.ic_book // R.drawable.ic_confirm 대신 적절한 아이콘으로 변경
                )

                OutlinedTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    label = { Text("책 제목 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_book),
                            contentDescription = "",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )

                StarRatingInput(
                    currentRating = rating,
                    onRatingChange = { rating = it },
                    modifier = Modifier.padding(top = 8.dp)
                )

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("나의 리뷰 *") },
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

            // --- 추가 정보 섹션 ---
            ReviewSectionCard {
                SectionTitle(text = "책 기본 정보 (선택)")

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
            }

            // --- 독서 기간 섹션 ---
            ReviewSectionCard {
                SectionTitle(text = "나의 독서 기록 (선택)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DateInputTextField(
                        label = "읽기 시작한 날",
                        date = startDate.toFormattedString(),
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    DateInputTextField(
                        label = "다 읽은 날",
                        date = endDate.toFormattedString(),
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

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

            Spacer(modifier = Modifier.height(80.dp)) // FAB를 위한 공간 확보
        }
    }

    // --- DatePicker Dialogs ---
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

@Composable
fun ImagePickerSection(
    selectedImageUri: Uri?,
    onImagePickerClick: () -> Unit,
    onImageClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onImagePickerClick),
        contentAlignment = Alignment.Center
    ) {
        if (selectedImageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(selectedImageUri),
                contentDescription = stringResource(R.string.cd_selected_book_cover),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onImageClearClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_clear_image), tint = Color.White)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_confirm),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.label_add_book_cover),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.label_tap_to_add_image),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


@Composable
fun ReviewSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Card 내부 요소 간 간격
        ) {
            content()
        }
    }
}

@Composable
fun SectionTitle(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(bottom = 0.dp, top = 0.dp), // 제목과 첫 필드 간격 조정
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInputTextField(
    label: String,
    date: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = date,
        onValueChange = { /* readOnly, no-op */ },
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_today),
                contentDescription = "저장",
                modifier = Modifier.size(24.dp)
            )
        },
        readOnly = true,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            // 포커스 안 되도록 보이게 하기 위함
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        placeholder = { Text("날짜 선택") } // Placeholder 추가
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePickerDialog(
    datePickerState: DatePickerState,
    onDismiss: () -> Unit,
    onDateSelected: (Long?) -> Unit,
    confirmEnabled: Boolean // State<Boolean>으로 변경
) {
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            // MyDatePickerDialog Composable 내부

            TextButton(
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                },
                enabled = confirmEnabled // .value 제거
            ) {
                Text(stringResource(R.string.button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun StarRatingInput(
    maxStars: Int = 5,
    currentRating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 36.dp, // 별 크기 조정
    starColor: Color = MaterialTheme.colorScheme.primary,
    emptyStarColor: Color = MaterialTheme.colorScheme.outline
) {
    Column(modifier = modifier) {
        Text(
            text = "나의 별점 *",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 1..maxStars) {
                val isSelected = i <= currentRating
                val icon = if (isSelected)  R.drawable.ic_heart  else R.drawable.ic_fill_heart // 채워진 별, 빈 별
                Icon(
                    painterResource(icon),
                    contentDescription = null, // 각 별에 대한 설명은 필요 없을 수 있음
                    tint = if (isSelected) starColor else emptyStarColor,
                    modifier = Modifier
                        .size(starSize)
                        .clickable { onRatingChange(i.toFloat()) }
                )
            }
        }
        if (currentRating > 0) {
            Text(
                text = "${currentRating.toInt()}점",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
