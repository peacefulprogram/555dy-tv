package io.github.peacefulprogram.dy555.compose.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import io.github.peacefulprogram.dy555.R


@Composable
fun Loading(text: String = "Loading"): Unit {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(text = text)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ErrorTip(message: String, retry: () -> Unit = { }) {
    val focusRequester = remember {
        FocusRequester()
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message)
        Button(
            onClick = retry, modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
        ) {
            Text(text = stringResource(R.string.button_retry))
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}