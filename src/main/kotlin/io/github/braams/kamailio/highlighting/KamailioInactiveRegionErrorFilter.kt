package io.github.braams.kamailio.highlighting

import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.psi.PsiErrorElement
import io.github.braams.kamailio.psi.KamailioFile

class KamailioInactiveRegionErrorFilter : HighlightErrorFilter() {
    override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
        if (element.containingFile !is KamailioFile) return true
        return !KamailioInactiveRegions.isInInactiveRegion(element)
    }
}
