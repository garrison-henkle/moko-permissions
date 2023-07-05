/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.permissions

open class PartiallyDeniedException(
    val granted: List<Permission>,
    message: String? = null,
) : Exception(message)

open class DeniedException(
    val permission: Permission,
    message: String? = null
) : Exception(message)

class DeniedAlwaysException(
    permission: Permission,
    message: String? = null
) : DeniedException(permission, message)
