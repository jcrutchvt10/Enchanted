package com.enchanted.app.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val clipboard: ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyToClipboard(text: String) {
        val clip = ClipData.newPlainText("enchanted", text)
        clipboard.setPrimaryClip(clip)
    }

    fun getClipboardText(): String? {
        if (!clipboard.hasPrimaryClip()) return null
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    }
}
