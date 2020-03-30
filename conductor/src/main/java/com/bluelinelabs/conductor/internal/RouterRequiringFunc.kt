package com.bluelinelabs.conductor.internal

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

@RestrictTo(LIBRARY_GROUP)
interface RouterRequiringFunc {
  fun execute()
}
