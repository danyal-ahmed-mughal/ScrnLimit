package com.scrnlimit.screen.time.control.block

import com.scrnlimit.screen.time.control.domain.BlockReason

enum class PauseMode { RITUAL, STOP }

data class PauseModel(
    val packageName: String,
    val label: String,
    val mode: PauseMode,
    val waitSeconds: Int,
    val burstMinutes: Int,
    val breath: Boolean,
    val reason: BlockReason,
    val detail: String,
    val passesLeft: Int,
)
