package com.peachspot.liteum.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier // 이미 fillParentMaxSize 또는 fillMaxWidth가 적용되어 전달됨
    ) {
        CircularProgressIndicator()
    }
}
