package pro.mezentsev.tracker.showcase.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun TestLayout(input: Int) {
    val testLayoutInput: MutableState<String> = remember(input) { mutableStateOf("Value: $input") }

    Column {
        Column {
            Column(Modifier.clickable { testLayoutInput.value += "!" }) {
                Text("TestLayout: ${testLayoutInput.value}")
            }
        }
    }
}

@Composable
fun SimpleLayout() {
    val test = remember { mutableIntStateOf(1) }
    val counter = remember { mutableIntStateOf(2) }

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
            Column {
                Text("Count: ${counter.intValue}")
            }
        }
    }
}