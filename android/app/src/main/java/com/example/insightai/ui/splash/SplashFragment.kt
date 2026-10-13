
package com.example.insightai.ui.splash

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.insightai.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val insightText = view.findViewById<TextView>(R.id.textInsight)
        val aiText = view.findViewById<TextView>(R.id.textAI)

        val screenWidth = resources.displayMetrics.widthPixels.toFloat()

        insightText.translationX = -screenWidth
        aiText.translationX = screenWidth

        ObjectAnimator.ofFloat(
            insightText,
            View.TRANSLATION_X,
            -screenWidth,
            0f
        ).apply {
            duration = 1200L
            start()
        }

        ObjectAnimator.ofFloat(
            aiText,
            View.TRANSLATION_X,
            screenWidth,
            0f
        ).apply {
            duration = 1200L
            start()
        }

        // Allow the text animation and Lottie animation to finish.
        viewLifecycleOwner.lifecycleScope.launch {
            delay(3500L)

            if (isAdded) {
                findNavController().navigate(
                    R.id.action_splashFragment_to_chatFragment
                )
            }
        }
    }
}
