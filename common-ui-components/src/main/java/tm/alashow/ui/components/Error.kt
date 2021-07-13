/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import tm.alashow.ui.Zoomable
import tm.alashow.ui.theme.AppTheme

@Composable
fun EmptyErrorBox(
    onRetryClick: () -> Unit = {},
    message: String = stringResource(R.string.error_empty),
    maxHeight: Dp? = null,
    maxHeightFraction: Float = 0.7f,
    modifier: Modifier = Modifier
) {
    ErrorBox(
        title = stringResource(R.string.error_empty_title),
        message = message,
        onRetryClick = onRetryClick,
        maxHeight = maxHeight,
        maxHeightPercent = maxHeightFraction,
        modifier = modifier
    )
}

@Preview
@Composable
fun ErrorBox(
    title: String = stringResource(R.string.error_title),
    message: String = stringResource(R.string.error_unknown),
    onRetryClick: () -> Unit = {},
    maxHeight: Dp? = null,
    maxHeightPercent: Float = 0.7f,
    modifier: Modifier = Modifier
) {
    ErrorBox(maxHeight, maxHeightPercent) {
        val loadingYOffset = (-70).dp
        val wavesComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.waves))

        Zoomable {
            LottieAnimation(
                wavesComposition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .size(1000.dp)
                    .offset(y = loadingYOffset)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.specs.paddingTiny),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = AppTheme.specs.paddingLarge)
        ) {
            Text(title, style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold))
            Text(message)
            TextRoundedButton(
                onClick = onRetryClick,
                text = stringResource(R.string.error_retry),
                modifier = Modifier.padding(top = AppTheme.specs.padding)
            )
        }
    }
}

@Composable
fun ErrorBox(
    maxHeight: Dp? = null,
    maxHeightPercent: Float = 0.7f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val boxModifier = when (maxHeight != null) {
        true -> modifier.height(maxHeight.times(maxHeightPercent))
        else -> modifier.fillMaxHeight()
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = boxModifier
            .fillMaxWidth()
    ) {
        content()
    }
}
