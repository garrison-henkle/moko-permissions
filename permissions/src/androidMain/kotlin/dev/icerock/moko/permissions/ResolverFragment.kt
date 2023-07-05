/*
 * Copyright 2021 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

internal class ResolverFragment : Fragment() {

    init {
        retainInstance = true
    }

    private var permissionCallback: PermissionCallback? = null

    private var allowPartialGrants = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionResults ->
            val permissionCallback = permissionCallback ?: return@registerForActivityResult
            this.permissionCallback = null

            val isCancelled = permissionResults.isEmpty()
            if (isCancelled) {
                permissionCallback.callback.invoke(
                    Result.failure(RequestCanceledException(permissionCallback.permission))
                )
                return@registerForActivityResult
            }

            val success = permissionResults.values.all { it }
            if (success) {
                permissionCallback.callback.invoke(Result.success(Unit))
            } else {
                val grantedPermissions = if(allowPartialGrants){
                    permissionResults.getPartialPermissionGrants()
                } else emptyList()
                when{
                    grantedPermissions.isNotEmpty() -> {
                        permissionCallback.callback.invoke(
                            Result.failure(PartiallyDeniedException(grantedPermissions))
                        )
                    }
                    shouldShowRequestPermissionRationale(permissionResults.keys.first()) -> {
                        permissionCallback.callback.invoke(
                            Result.failure(DeniedException(permissionCallback.permission))
                        )
                    }
                    else -> {
                        permissionCallback.callback.invoke(
                            Result.failure(DeniedAlwaysException(permissionCallback.permission))
                        )
                    }
                }
            }
        }

    fun requestPermission(
        permission: Permission,
        permissions: List<String>,
        allowPartialGrants: Boolean,
        callback: (Result<Unit>) -> Unit
    ) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                val toRequest = permissions.filter {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                }

                val androidS = Build.VERSION.SDK_INT > Build.VERSION_CODES.S

                if(
                    androidS &&
                    allowPartialGrants &&
                    permission == Permission.LOCATION &&
                    Manifest.permission.ACCESS_COARSE_LOCATION !in toRequest &&
                    Manifest.permission.ACCESS_FINE_LOCATION in toRequest
                ){
                    callback.invoke(
                        Result.failure(
                            PartiallyDeniedException(granted = listOf(Permission.COARSE_LOCATION))
                        )
                    )
                    return@repeatOnLifecycle
                }

                if (toRequest.isEmpty()) {
                    callback.invoke(Result.success(Unit))
                    return@repeatOnLifecycle
                }

                permissionCallback?.let {
                    it.callback.invoke(Result.failure(RequestCanceledException(it.permission)))
                    permissionCallback = null
                }

                this@ResolverFragment.allowPartialGrants = allowPartialGrants

                permissionCallback = PermissionCallback(permission, callback)

                requestPermissionLauncher.launch(toRequest.toTypedArray())
            }
        }
    }

    private class PermissionCallback(
        val permission: Permission,
        val callback: (Result<Unit>) -> Unit
    )

    private fun Map<String, Boolean>.getPartialPermissionGrants(): List<Permission>{
        val grantedPermissions = hashSetOf<Permission>()
        val grantedAndroidPermissions = toList().filter { it.second }.map { it.first }.toHashSet()
        if(grantedAndroidPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)){
            grantedPermissions += Permission.COARSE_LOCATION
        }
        return grantedPermissions.toList()
    }
}
