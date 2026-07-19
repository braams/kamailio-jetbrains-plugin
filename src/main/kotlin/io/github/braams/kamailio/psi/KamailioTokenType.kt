package io.github.braams.kamailio.psi

import com.intellij.psi.tree.IElementType
import io.github.braams.kamailio.KamailioLanguage
import org.jetbrains.annotations.NonNls

class KamailioTokenType(@NonNls debugName: String) : IElementType(debugName, KamailioLanguage)
