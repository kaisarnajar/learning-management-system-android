package com.darsequran.academy.data

import android.content.Context
import java.io.File

/** Where opened receipts and course files are written for the viewer app (shared through the FileProvider). */
fun documentCacheDir(context: Context): File = File(context.cacheDir, "documents")

/** Forgets every document the previous user opened. */
fun clearDocumentCache(context: Context) {
    documentCacheDir(context).deleteRecursively()
}
