/*
 * Copyright 2020-2020 the original author or authors from the JHapy project.
 *
 * This file is part of the JHapy project, see https://www.jhapy.org/ for more information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jhapy.resource.domain

import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

/**
 * @author jHapy Lead Dev.
 * @version 1.0
 * @since 2019-05-15
 */
@Document(collection = "storedFile")
class StoredFile : BaseEntity() {
    var filename: String? = null
    var mimeType: String? = null
    var filesize: Long = 0
    var content: ByteArray? = null
    var contentFileId: String? = null
    var md5Content: ByteArray? = null
    var orginalContent: ByteArray? = null
    var originalContentFileId: String? = null
    var pdfConvertStatus: PdfConvertEnum? = null
    var pdfContent: ByteArray? = null
    var pdfContentFileId: String? = null
    var metadata: Map<String, String> = HashMap()
    var relatedObjectId: UUID? = null
    var relatedObjectClass: String? = null
}

enum class PdfConvertEnum {
    NOT_CONVERTED, CONVERTED, NOT_SUPPORTED, NOT_NEEDED
}