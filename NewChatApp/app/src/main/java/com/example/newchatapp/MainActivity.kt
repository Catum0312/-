package com.example.newchatapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.SharedPreferences
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate


class MainActivity : AppCompatActivity() {

    companion object {
        lateinit var prefs: SharedPreferences
    }

    private lateinit var textViewContent: EditText
    private lateinit var btnVoice: ImageButton
    private lateinit var btnPlus: ImageButton
    private lateinit var btnThemeToggle: Button
    private lateinit var fragmentContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

// ✅ 전역 prefs 설정 및 테마 적용
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )


        textViewContent = findViewById(R.id.textViewContent)
        btnVoice = findViewById(R.id.btnVoice)
        btnPlus = findViewById(R.id.btnPlus)
        btnThemeToggle = findViewById(R.id.btnThemeToggle)
        fragmentContainer = findViewById(R.id.fragmentContainer)

        // ✅ 저장된 텍스트 불러오기 (같은 prefs 사용)
        val savedText = prefs.getString("main_text", "")
        textViewContent.setText(savedText)

        textViewContent.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val input = v.text.toString()
                textViewContent.append("\n$input")
                v.setText("")
                true
            } else false
        }

        // ✅ 다크모드/라이트모드 전환
        btnThemeToggle.setOnClickListener {
            val isDark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
            val editor = prefs.edit()
            if (isDark) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor.putBoolean("dark_mode", false)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor.putBoolean("dark_mode", true)
            }
            editor.apply()
            recreate()
        }

        btnPlus.setOnClickListener {
            val popup = androidx.appcompat.widget.PopupMenu(this, it)
            popup.menu.add("챗봇")
            popup.menu.add("필드 화면")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "챗봇" -> {
                        textViewContent.append("\n[AI 기능은 나중에 연결됩니다.]")
                        true
                    }
                    "필드 화면" -> {
                        textViewContent.visibility = View.GONE
                        fragmentContainer.visibility = View.VISIBLE
                        btnPlus.visibility = View.GONE
                        btnVoice.visibility = View.GONE
                        btnThemeToggle.visibility = View.GONE
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, MainFragment.newInstance())
                            .addToBackStack(null)
                            .commit()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        btnVoice.setOnClickListener {
            textViewContent.append("\n[음성 인식 기능은 나중에 연결됩니다.]")
        }
    }

    override fun onPause() {
        super.onPause()
        prefs.edit().putString("main_text", textViewContent.text.toString()).apply()
    }

    override fun onResume() {
        super.onResume()
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (current == null) {
            btnThemeToggle.visibility = View.VISIBLE
            btnPlus.visibility = View.VISIBLE
            btnVoice.visibility = View.VISIBLE
            textViewContent.visibility = View.VISIBLE
        }
    }

    fun restoreMainUI() {
        textViewContent.visibility = View.VISIBLE
        btnPlus.visibility = View.VISIBLE
        btnVoice.visibility = View.VISIBLE
        btnThemeToggle.visibility = View.VISIBLE
        fragmentContainer.visibility = View.GONE
    }

    fun hideThemeToggle() {
        btnThemeToggle.visibility = View.GONE
    }

    fun showThemeToggle() {
        btnThemeToggle.visibility = View.VISIBLE
    }
}
