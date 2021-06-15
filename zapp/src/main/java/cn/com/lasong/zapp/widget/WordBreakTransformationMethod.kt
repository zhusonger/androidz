package cn.com.lasong.zapp.widget

import android.text.method.ReplacementTransformationMethod

object WordBreakTransformationMethod : ReplacementTransformationMethod() {
    private val dash = charArrayOf('-', '\u2011')
    private val space = charArrayOf(' ', '\u00A0')
    private val slash = charArrayOf('/', '\u2215')

    private val original = charArrayOf(dash[0], space[0], slash[0])
    private val replacement = charArrayOf(dash[1], space[1], slash[1])

    override fun getOriginal() = original
    override fun getReplacement() = replacement
}