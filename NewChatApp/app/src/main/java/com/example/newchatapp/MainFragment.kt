package com.example.newchatapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.activityViewModels
import com.example.field.CommandDetailFragment
import com.example.field.EasyCommandCategoryAdapter
import com.example.field.EasyCommandEntryAdapter
import com.example.field.Field
import com.example.field.FieldAdapter
import com.example.field.FieldDetailFragment
import com.example.field.FieldViewModel

class MainFragment : Fragment(R.layout.fragment_main) {

    private lateinit var editTextInput: EditText
    private lateinit var btnPlus: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerCategory: RecyclerView
    private lateinit var recyclerEntry: RecyclerView
    private lateinit var btnDropdown: ImageButton
    private val viewModel: FieldViewModel by activityViewModels()

    private val easyCommandMap = mutableMapOf(
        "요약" to mutableListOf("간단 요약: 이 내용을 간단하게 요약해줘"),
        "다듬기" to mutableListOf("문장 정리: 더 자연스럽게 바꿔줘"),
        "늘리기" to mutableListOf("내용 확장: 더 길게 써줘")
    )

    private var currentCategory: String = easyCommandMap.keys.first()
    private lateinit var categoryAdapter: EasyCommandCategoryAdapter
    private lateinit var entryAdapter: EasyCommandEntryAdapter

    // 필드 목록
    private val fields = listOf(
        Field("목적", ""), Field("주제", ""), Field("독자", ""), Field("형식 혹은 구조", ""),
        Field("근거자료", ""), Field("어조", ""), Field("분량, 문체, 금지어 등", ""), Field("추가사항", "")
    )

    private val fieldKeys = listOf(
        "목적","주제","독자","형식 혹은 구조",
        "근거자료","어조","분량, 문체, 금지어 등","추가사항"
    )

    private val fieldTextMap = mutableMapOf<String, String>()
    private var currentField: Field? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTextInput   = view.findViewById(R.id.editTextInput)
        btnPlus         = view.findViewById(R.id.btnPlus)
        recyclerView    = view.findViewById(R.id.recyclerView)
        recyclerCategory= view.findViewById(R.id.recyclerEasyCommand)
        recyclerEntry   = view.findViewById(R.id.recyclerCommandEntry)
        btnDropdown = view.findViewById(R.id.btnFieldDropdown)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        // ViewModel 기본 제목 초기화 (한 번만)
        viewModel.initTitles(fieldKeys)

        // ② 드롭다운 메뉴: ViewModel 에 저장된 제목(label) 사용
        btnDropdown.setOnClickListener { anchor ->
            PopupMenu(requireContext(), anchor).apply {
                // menu에 label(=getTitle) 추가
                fieldKeys.forEach { key ->
                    menu.add(viewModel.getTitle(key))
                }
                setOnMenuItemClickListener { item ->
                    // 선택된 label → 원본 key 찾기
                    val selectedLabel = item.title.toString()
                    val key = fieldKeys.first { viewModel.getTitle(it) == selectedLabel }

                    // ③ 상세화면으로 이동, content도 동기화
                    val frag = FieldDetailFragment.newInstance(
                        key,
                        viewModel.getContent(key)
                    )
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, frag)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                show()
            }
        }

        // **1) ViewModel에 기본 필드명 채우기**
        val fieldKeys = fields.map { it.title }
        viewModel.initTitles(fieldKeys)

        // RecyclerView 설정
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val fieldAdapter = FieldAdapter(
            fields = fields,
            titleMap   = viewModel.titles.value   ?: emptyMap(),
            contentMap = viewModel.contents.value ?: emptyMap()
        ) { field ->
            // 상세 화면으로 이동 (MainFragment에서 내용 저장 제거)
            val frag = FieldDetailFragment.newInstance(
                field.title,
                viewModel.getContent(field.title)
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, frag)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = fieldAdapter

        // 2) LiveData 관찰 → 리스트 갱신
        viewModel.titles.observe(viewLifecycleOwner)   { fieldAdapter.notifyDataSetChanged() }
        viewModel.contents.observe(viewLifecycleOwner) { fieldAdapter.notifyDataSetChanged() }

        // EasyCommand 리스트 기본 숨김
        recyclerCategory.visibility = View.GONE
        recyclerEntry.visibility   = View.GONE

        // EditText에 포커스 생길 때만 EasyCommand 보이기
        editTextInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                recyclerCategory.visibility = View.VISIBLE
                recyclerEntry.visibility   = View.VISIBLE
            } else {
                recyclerCategory.visibility = View.GONE
                recyclerEntry.visibility   = View.GONE
            }
        }

        // 상단 EasyCommand 카테고리
        recyclerCategory.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        categoryAdapter = EasyCommandCategoryAdapter(
            easyCommandMap.keys.toList(),
            onCategoryClick = { selected ->
                currentCategory = selected
                updateEntryList()
            },
            onDeleteConfirmed = { category ->
                easyCommandMap.remove(category)
                refreshCategoryAdapter()
            }
        ).apply {
            setOnAddCategoryClickListener { showAddCategoryDialog() }
        }
        recyclerCategory.adapter = categoryAdapter

        // 하단 EasyCommand 엔트리
        recyclerEntry.layoutManager = LinearLayoutManager(requireContext())
        entryAdapter = EasyCommandEntryAdapter(
            entries = emptyList(),
            onEditClick = { title, prompt -> openDetail(title, prompt) },
            onDeleteClick = { title ->
                easyCommandMap[currentCategory]?.removeIf { it.startsWith(title) }
                updateEntryList()
            },
            onPromptClick = { prompt -> editTextInput.setText(prompt) },
            onAddClick = { openDetail(null, null) }
        )
        recyclerEntry.adapter = entryAdapter

        // ➕ 버튼 팝업 메뉴
        btnPlus.setOnClickListener {
            PopupMenu(requireContext(), it).apply {
                menu.add("챗봇").setOnMenuItemClickListener {
                    Toast.makeText(requireContext(), "아직 만들고 있습니다", Toast.LENGTH_SHORT).show()
                    true
                }
                menu.add("글 편집 화면").setOnMenuItemClickListener {
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    true
                }
                show()
            }
        }

        // 초기 엔트리 리스트 세팅
        updateEntryList()

        // **5+7. CommandDetailFragment에서 온 저장 결과 받기**
        parentFragmentManager.setFragmentResultListener(
            "easy_command_save", viewLifecycleOwner
        ) { _, bundle ->
            val original = bundle.getString("original") ?: ""
            val title = bundle.getString("title") ?: return@setFragmentResultListener
            val prompt = bundle.getString("prompt") ?: return@setFragmentResultListener
            easyCommandMap[currentCategory]?.let { commands ->       // 바깥 it → commands 로 변경
                if (original.isNotBlank()) {
                    commands.removeIf { entry ->                      // 중첩 람다의 it → entry 로 변경
                        entry.startsWith("$original:")                // 문자열 템플릿 사용
                    }
                }
                // 새로 추가
                commands.add("$title:$prompt")
            }

            updateEntryList()
        }

        // 2) 루트 레이아웃 터치 시 포커스 해제
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()              // ← 클릭 처리(접근성용)
            }
            editTextInput.clearFocus()
            false
        }


        // **1. 포커스 해제 시 숨김 (이미 구현)**
        editTextInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                recyclerCategory.visibility = View.VISIBLE
                recyclerEntry.visibility = View.VISIBLE
            } else {
                recyclerCategory.visibility = View.GONE
                recyclerEntry.visibility = View.GONE
            }
        }

        // 백버튼 누를 때
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (editTextInput.hasFocus()) {
                // EditText에 포커스가 남아 있으면 → 해제만
                editTextInput.clearFocus()
            } else {
                // 그 외엔 원래 백버튼 동작
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        view.findViewById<ImageButton>(R.id.btnSend).setOnClickListener {
            // 1) 현재 필드로 입력 내용 저장
            currentField?.let { f ->
                fieldTextMap[f.title] = editTextInput.text.toString()
            }
            // 2) 입력창 초기화
            editTextInput.text.clear()
            // 3) RecyclerView 갱신 (preview 반영)
            recyclerView.adapter?.notifyDataSetChanged()
        }

    }

    private fun updateEntryList() {
        val list = easyCommandMap[currentCategory]?.map {
            val parts = it.split(":")
            parts[0] to parts.getOrNull(1).orEmpty()
        }?.toMutableList() ?: mutableListOf()
        list.add("+" to "")
        entryAdapter.updateList(list)
    }

    private fun refreshCategoryAdapter() {
        categoryAdapter = EasyCommandCategoryAdapter(
            easyCommandMap.keys.toList(),
            categoryAdapter.onCategoryClick,
            categoryAdapter.onDeleteConfirmed
        ).apply { setOnAddCategoryClickListener { showAddCategoryDialog() } }
        recyclerCategory.adapter = categoryAdapter
        updateEntryList()
    }

    private fun showAddCategoryDialog() {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("카테고리 추가")
            .setView(input)
            .setPositiveButton("추가") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank() && !easyCommandMap.containsKey(name)) {
                    easyCommandMap[name] = mutableListOf()
                    refreshCategoryAdapter()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun openDetail(title: String?, prompt: String?) {
        CommandDetailFragment.newInstance(currentCategory, title, prompt)
            .show(parentFragmentManager, "CommandDetail")
    }

    companion object {
        fun newInstance(): MainFragment = MainFragment()
    }
}
