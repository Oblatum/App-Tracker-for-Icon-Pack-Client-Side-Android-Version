package ren.imyan.app_tracker.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import ren.imyan.app_tracker.databinding.DialogUploadBinding

class UploadDialog : DialogFragment() {

    private var binding: DialogUploadBinding? = null
    private var total:Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogUploadBinding.inflate(inflater, container, false)
        return binding?.root
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        binding?.progressText?.text = "当前已上传 0/$total 个"
    }

    @SuppressLint("SetTextI18n")
    fun setTotal(total:Int) {
        this.total = total
    }

    @SuppressLint("SetTextI18n")
    fun updateProgress(progress: Int) {
        binding?.progressText?.text = "当前已上传 $progress/$total 个"
    }
}