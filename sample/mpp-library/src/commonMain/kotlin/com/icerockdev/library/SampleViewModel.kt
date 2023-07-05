package com.icerockdev.library

import dev.icerock.moko.mvvm.dispatcher.EventsDispatcher
import dev.icerock.moko.mvvm.dispatcher.EventsDispatcherOwner
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.PartiallyDeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.launch

class SampleViewModel(
    override val eventsDispatcher: EventsDispatcher<EventListener>,
    val permissionsController: PermissionsController,
    private val permissionType: Permission,
    val onResultCallback: (PermissionState) -> Unit = {},
    val callback: (msg: String) -> Unit = {},
) : ViewModel(), EventsDispatcherOwner<SampleViewModel.EventListener> {

    init {
        viewModelScope.launch {
            val startState = permissionsController.getPermissionState(permissionType)
            println(startState)
        }
    }

    /**
     * An example of using [PermissionsController] in common code.
     */
    fun onRequestPermissionButtonPressed() {
        requestPermission(permissionType)
    }

    private fun requestPermission(permission: Permission, requirePreciseLocation: Boolean = false) {
        viewModelScope.launch {
            try {
                permissionsController.getPermissionState(permission)
                    .also { println("pre provide $it") }

                // Calls suspend function in a coroutine to request some permission.

                permissionsController.providePermission(
                    permission = permission,
                    allowPartialAndroidGrants = !requirePreciseLocation
                )

                // If there are no exceptions, permission has been granted successfully.

                eventsDispatcher.dispatchEvent { onSuccess() }
            } catch (partiallyDeniedException: PartiallyDeniedException) {
                eventsDispatcher.dispatchEvent { onPartiallyDenied(partiallyDeniedException) }
            } catch (deniedAlwaysException: DeniedAlwaysException) {
                eventsDispatcher.dispatchEvent { onDeniedAlways(deniedAlwaysException) }
            } catch (deniedException: DeniedException) {
                eventsDispatcher.dispatchEvent { onDenied(deniedException) }
            } finally {
                val result = permissionsController.getPermissionState(permission)
                    .also { println("post provide $it") }
                onResultCallback(result)
                callback("after 3")
            }
        }
    }

    interface EventListener {

        fun onSuccess()

        fun onDenied(exception: DeniedException)

        fun onPartiallyDenied(exception: PartiallyDeniedException)

        fun onDeniedAlways(exception: DeniedAlwaysException)
    }
}
