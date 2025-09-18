package com.peachspot.liteum.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peachspot.liteum.R

@Composable
fun StarRatingInput(
    maxStars: Int = 5,
    currentRating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 32.dp,
    starColor: Color = Color(0xFFd80000) ,
    emptyStarColor: Color =  Color.Gray
) {
    Column(modifier = modifier) {
        Text(
            text = "만족도 *",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (starIndex in 1..maxStars) { // 1부터 5까지 (또는 maxStars까지)
                // 현재 별(starIndex)이 사용자의 currentRating보다 작거나 같으면 채워진 별
                val isSelected = starIndex.toFloat() <= currentRating
                // 아이콘 리소스 R.drawable.ic_heart (채워진 별)와 R.drawable.ic_fill_heart (빈 별)가 필요합니다.
                // 만약 ic_fill_heart가 채워진 것이라면 아래 로직을 반대로 해야 합니다.
                // 일반적으로 'fill'이 채워진 것을 의미하므로, isSelected일 때 ic_fill_heart를 사용한다고 가정합니다.
                // 가지고 계신 아이콘 리소스에 맞게 수정해주세요.
                val iconRes = if (isSelected) R.drawable.ic_fill_heart else R.drawable.ic_heart // 사용자 아이콘에 맞게 수정
                Icon(
                    painterResource(iconRes),
                    contentDescription = null, // 필요시 "별 $starIndex" 등으로 구체화
                    tint = if (isSelected) starColor else emptyStarColor,
                    modifier = Modifier
                        .size(starSize)
                        .clickable {
                            onRatingChange(starIndex.toFloat()) // 클릭된 별의 값(1, 2, 3, 4, 5)을 전달
                        }
                )
            }
        }
//        if (currentRating > 0) {
//            Text(
//                text = stringResource(R.string.rating_score, currentRating.toInt()),
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                modifier = Modifier.padding(start = 2.dp, top = 4.dp)
//            )
//        }
    }
}
