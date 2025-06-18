package com.peachspot.legendkofarm.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalTextApi::class)
@Composable
fun PrivacyPolicyLink() {
    val uriHandler = LocalUriHandler.current
    val privacyPolicyUrl =
        "https://peachspot.co.kr/oroogi/privacy" // Replace with your actual URL

    val annotatedString = buildAnnotatedString {
        append(" [")
        // Apply pushLink without textDecoration here
        pushLink(LinkAnnotation.Url(privacyPolicyUrl))
        withStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,// Color(0xFF0c275a), // Consider using MaterialTheme.colorScheme.primary
                fontSize = 16.sp,
                textDecoration = TextDecoration.Underline // Apply textDecoration in SpanStyle
            )
        ) {
            append("개인정보취급방침 보기")
        }
        pop() // Corresponds to pushLink
        append("]")
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier, // Add any necessary modifiers
        onTextLayout = { /* If you need text layout results */ },
        // The onClick for Text with LinkAnnotation is handled automatically
        // by the system to open the link when the linked part is clicked.
        // You no longer need the manual onClick lambda for URI handling here.
    )
}

