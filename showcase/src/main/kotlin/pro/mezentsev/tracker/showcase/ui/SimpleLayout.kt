package pro.mezentsev.tracker.showcase.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TestLayout(input: Int) {
    val testLayoutInput = remember(input) { mutableStateOf("Value: $input") }

    Column(Modifier.clickable { testLayoutInput.value += "!" }) {
        Text("TestLayout: ${testLayoutInput.value}")
    }
}

@Composable
fun SimpleLayout() {
    val test = remember { mutableIntStateOf(1) }
    val counter = remember { mutableIntStateOf(2) }

    val color = remember { Animatable(Color.Gray) }
    LaunchedEffect(counter.intValue) {
        if (counter.intValue % 2f == 0f) {
            color.animateTo(Color.Red)
        } else {
            color.animateTo(Color.Yellow)
        }
    }
    //val width = animateDpAsState(targetValue = (counter.intValue + 100).dp )

    Column {
        Button(onClick = { test.intValue++ }) {
            Column {
                Text("Test: ${test.intValue}")
            }
        }

        if (counter.intValue > 3) {
            TestLayout(counter.intValue)
        }

        Button(onClick = { counter.intValue++ }) {
            Text("Count: ${counter.intValue}", color = color.value)
        }
    }
}