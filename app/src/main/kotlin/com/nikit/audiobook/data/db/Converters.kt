package com.nikit.audiobook.data.db

import androidx.room.TypeConverter
import com.nikit.audiobook.domain.model.BookStatus
import com.nikit.audiobook.domain.model.FileType
import com.nikit.audiobook.domain.model.SourceKind

class Converters {
    @TypeConverter fun fileTypeToString(v: FileType?): String? = v?.name

    @TypeConverter fun stringToFileType(v: String?): FileType? = v?.let { FileType.valueOf(it) }

    @TypeConverter fun bookStatusToString(v: BookStatus?): String? = v?.name

    @TypeConverter fun stringToBookStatus(v: String?): BookStatus? = v?.let { BookStatus.valueOf(it) }

    @TypeConverter fun sourceKindToString(v: SourceKind?): String? = v?.name

    @TypeConverter fun stringToSourceKind(v: String?): SourceKind? = v?.let { SourceKind.valueOf(it) }
}
