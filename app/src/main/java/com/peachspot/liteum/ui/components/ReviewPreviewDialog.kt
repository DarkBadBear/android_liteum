package com.peachspot.liteum.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults // Material 3의 DividerDefaults 사용
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider // Material 3의 HorizontalDivider 사용
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties // DialogProperties import
import com.peachspot.liteum.data.model.BookReview
import com.peachspot.liteum.data.model.FeedItem

// 데이터 클래스 정의 (이미 프로젝트에 있을 것으로 가정)
// data class FeedItem(val bookTitle: String, val postImageUrl: String? = null, /* ... */)
// data class BookReview(val id: String, val rating: Float, val reviewText: String, val content: String, /* ... */)


@Composable
fun ReviewPreviewDialog(
    feedItem: FeedItem,
    review: BookReview,
    onDismissRequest: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 플랫폼 기본 너비 제한 해제
            dismissOnBackPress = true, // 백 버튼으로 닫기 활성화 (선택 사항)
            dismissOnClickOutside = true // 바깥 영역 클릭으로 닫기 활성화 (선택 사항)
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize() // 화면 전체를 채우도록 설정
            // .padding(16.dp) // 전체 화면에서는 이 패딩이 적절하지 않을 수 있음, 필요에 따라 조정
            ,
            shape = MaterialTheme.shapes.large, // 모서리 모양은 유지하거나 MaterialTheme.shapes.extraSmall 등으로 변경 가능
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // 전체 화면일 경우 그림자가 필요 없을 수 있음
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize() // Column도 전체 크기를 차지하도록
                    .padding(16.dp) // 카드 내부 컨텐츠 패딩은 유지
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 제목 텍스트 (좌측 공간을 더 차지하도록 weight 사용 가능)
                    Text(
                        text = feedItem.bookTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f) // 메뉴 버튼 외의 공간을 채우도록
                    )
                    // 메뉴 버튼 (우측에 고정)
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
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("삭제") },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness, // Material 3의 기본 두께
                    color = DividerDefaults.color // Material 3의 기본 색상
                )

                Text(
                    text = "리뷰 (평점: ${"%.1f".format(review.rating)})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                // 스크롤 가능한 리뷰 내용 (내용이 길 경우 대비)
                Column(modifier = Modifier.weight(1f)) { // 남은 공간을 모두 차지하도록
                    Text(
                        text = review.reviewText.takeIf { it.isNotBlank() } ?: review.content,
                        style = MaterialTheme.typography.bodyMedium,
                        // maxLines = 10, // 전체 화면에서는 더 많은 줄을 보여주거나 스크롤 처리
                        // overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("닫기")
                    }
                }
            }
        }
    }
}
