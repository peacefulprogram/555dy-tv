package io.github.peacefulprogram.dy555.ext

import android.widget.Toast
import io.github.peacefulprogram.dy555.Dy555Application

fun String.showToast(duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(Dy555Application.context, this, duration).show()
}