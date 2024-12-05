/*
 * Copyright 2024 Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartlook.app.ui.wireframe

import android.content.Intent
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.HtmlCompat
import com.smartlook.app.R
import com.smartlook.app.databinding.FragmentWireframeViewsBinding
import com.smartlook.app.ui.BaseFragment
import com.smartlook.app.ui.adapter.SimpleItemAdapter
import com.smartlook.app.ui.adapter.SpinnerAdapter
import com.smartlook.app.ui.adapter.ViewPagerAdapter
import com.smartlook.app.ui.adapter.ViewPagerAdapter2
import com.smartlook.app.ui.dialog.AlertDialog
import com.smartlook.app.ui.dialog.BottomSheetDialogFragment
import com.smartlook.app.ui.dialog.DialogActivity
import com.smartlook.app.ui.dialog.DialogFragment
import com.smartlook.app.ui.wireframe.model.SimpleItem
import com.smartlook.app.util.randomColor
import com.smartlook.sdk.common.utils.dpToPx

// FIXME CalendarView is broken on Android 5.0
class WireframeViewsFragment : BaseFragment<FragmentWireframeViewsBinding>() {

    override val titleRes: Int = R.string.wireframe_title
    override val subtitleRes: Int = R.string.wireframe_native_subtitle

    override val viewBindingCreator: (LayoutInflater, ViewGroup?, Boolean) -> FragmentWireframeViewsBinding
        get() = FragmentWireframeViewsBinding::inflate

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.googleMap3.onCreate(savedInstanceState)
        setup()
    }

    override fun onStart() {
        super.onStart()
        viewBinding.googleMap3.onStart()
    }

    override fun onResume() {
        super.onResume()
        viewBinding.googleMap3.onResume()
        viewBinding.surfaceView.onResume()
    }

    override fun onPause() {
        viewBinding.googleMap3.onPause()
        viewBinding.surfaceView.onPause()
        super.onPause()
    }

    override fun onStop() {
        viewBinding.googleMap3.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        viewBinding.googleMap3.onDestroy()
        super.onDestroyView()
    }

    private fun setup() {
        viewBinding.toast1.setOnClickListener(onClickListener)

        viewBinding.viewPager1.adapter = ViewPagerAdapter(childFragmentManager)
        viewBinding.viewPager2.adapter = ViewPagerAdapter2(this)

        viewBinding.textInputLayout1.error = "Is it really your name?"

        val imageMatrix = Matrix()
        imageMatrix.preScale(2f, 1f, 0.5f, 0.5f)
        imageMatrix.preTranslate(60f, 50f)

        viewBinding.imageView11.imageMatrix = imageMatrix
        viewBinding.imageView12.imageMatrix = imageMatrix
        viewBinding.imageView13.imageMatrix = imageMatrix
        viewBinding.imageView14.imageMatrix = imageMatrix

        val list = (0..10).map { SimpleItem(it, "Title $it", "Description of $it", randomColor()) }
        val recyclerViewAdapter = SimpleItemAdapter(requireContext(), list)

        viewBinding.recyclerView1.adapter = recyclerViewAdapter
        viewBinding.recyclerView2.adapter = recyclerViewAdapter

        val items = (0..3).map { SimpleItem(it, "Title $it", "Description of $it", randomColor()) }
        val spinnerAdapter = SpinnerAdapter(requireContext(), items)

        viewBinding.spinner1.adapter = spinnerAdapter
        viewBinding.spinner2.adapter = spinnerAdapter

        viewBinding.dialog1.setOnClickListener(onClickListener)
        viewBinding.dialog2.setOnClickListener(onClickListener)
        viewBinding.dialog3.setOnClickListener(onClickListener)
        viewBinding.dialog4.setOnClickListener(onClickListener)

        viewBinding.textView5.setHorizontallyScrolling(true)

        viewBinding.numberPicker1.minValue = 0
        viewBinding.numberPicker1.maxValue = 10

        viewBinding.textView6.text = HtmlCompat.fromHtml("<span style='color:red'>Red text</span> and <big>BIG TEXT</big> and <small>small text</small> and <b>bold text</b> and <span style='background-color:green; color:blue'>text with background</span> and <a href='www.smartlook.com'>link to www.smartlook.com</a><br/><ul><li><small>Coffee</small></li><li>Tea</li><li><big>Milk</big></li></ul><br/>This text contains <sub>subscript</sub> text.<br/>This text contains <sup>superscript</sup> text.<br/>", HtmlCompat.FROM_HTML_MODE_COMPACT)

        viewBinding.textViewSpans.text = createTextWithSpans()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N && Build.VERSION.SDK_INT != Build.VERSION_CODES.O) // Check was added later, null on layer causes a crash
            viewBinding.viewEmptyLayer.background = LayerDrawable(arrayOf(ColorDrawable(Color.RED), null, ColorDrawable(Color.RED)))
    }

    private fun createTextWithSpans(): Spannable {
        val spannable = SpannableStringBuilder()
        spannable.append("Intermediary bank SWIFT/BIC  #")
        spannable.setSpan(createIconSpan(), spannable.length - 1, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.append("  (optional)")

        val clickSpan = object : ClickableSpan() {
            override fun onClick(view: View) {}

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(clickSpan, 0, spannable.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        return spannable
    }

    private fun createIconSpan(): ImageSpan {
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_add, requireContext().theme)?.mutate() ?: throw IllegalArgumentException()
        val size = dpToPx(16f)

        drawable.setBounds(0, 0, size, size)
        DrawableCompat.setTint(drawable, Color.RED)

        return ImageSpan(drawable)
    }

    private val onClickListener = View.OnClickListener {
        when (it.id) {
            viewBinding.toast1.id ->
                Toast.makeText(requireContext(), "Sample Toast", Toast.LENGTH_LONG).show()
            viewBinding.dialog1.id ->
                AlertDialog.show(requireContext())
            viewBinding.dialog2.id ->
                DialogFragment().show(childFragmentManager, "DialogFragment")
            viewBinding.dialog3.id ->
                startActivity(Intent(requireContext(), DialogActivity::class.java))
            viewBinding.dialog4.id ->
                BottomSheetDialogFragment().show(childFragmentManager, "BottomSheetDialogFragment")
        }
    }
}
