package com.example.csmaster

import android.text.Editable
import android.text.TextWatcher

class SimpleTextWatcher(private val onChanged: (String) -> Unit) : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        onChanged(s?.toString()?.trim() ?: "")
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}
