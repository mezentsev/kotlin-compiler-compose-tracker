package pro.mezentsev.tracker.showcase.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember

@Composable
fun TestLayout() {
    val test = remember { mutableIntStateOf(0) }

    Column {
        Button(onClick = { test.intValue++ }) {
            Column {
                Text("Test: ${test.intValue}")
            }
        }
    }
}

@Composable
fun SimpleLayout() {
    val counter = remember { mutableIntStateOf(0) }

    Column {
        TestLayout()

        Button(onClick = { counter.intValue++ }) {
            Column {
                Text("Count: ${counter.intValue}")
            }
        }
    }
}