package com.mothakrati.app

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("rooh_prefs", 0) }
    private var currentScreen = "HOME"
    private var section = "german"
    private var editIndex = -1
    private var selectedImageUri: String? = null
    private var imagePreview: ImageView? = null

    private val titles = mapOf(
        "german" to "🇩🇪 الألمانية",
        "programming" to "🐍 البرمجة",
        "work" to "👨‍🍳 العمل / الوصفات",
        "ideas" to "💡 أفكاري"
    )

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedImageUri = it.toString()
            imagePreview?.setImageURI(it)
            imagePreview?.visibility = View.VISIBLE
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        render()
    }

    private fun render() {
        when (currentScreen) {
            "HOME" -> showHomeScreen()
            "SECTION" -> showSectionScreen()
            "EDITOR" -> showEditorScreen()
        }
    }

    // --- 1. الشاشة الرئيسية (عمودية) ---
    private fun showHomeScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val titleText = TextView(this).apply {
            text = "تطبيق روح - ROOH"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1F2937"))
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }
        root.addView(titleText)

        val subTitle = TextView(this).apply {
            text = "اختر القسم للمتابعة:"
            textSize = 16f
            setTextColor(Color.parseColor("#4B5563"))
            setPadding(8, 0, 8, 16)
        }
        root.addView(subTitle)

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.forEach { (key, label) ->
            val btn = Button(this).apply {
                text = label
                textSize = 18f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#6366F1"))
                    cornerRadius = 20f
                }
                setPadding(24, 24, 24, 24)
                setOnClickListener {
                    section = key
                    currentScreen = "SECTION"
                    render()
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 12, 0, 12) }
            btn.layoutParams = params
            list.addView(btn)
        }

        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)
    }

    // --- 2. صفحة القسم المستقلة ---
    private fun showSectionScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 16)
        }

        val backBtn = Button(this).apply {
            text = "⬅️ عودة"
            setOnClickListener {
                currentScreen = "HOME"
                render()
            }
        }
        header.addView(backBtn)

        val title = TextView(this).apply {
            text = titles[section]
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#111827"))
            setPadding(20, 0, 0, 0)
        }
        header.addView(title)
        root.addView(header)

        val addBtn = Button(this).apply {
            text = if (section == "work") "➕ إضافة وصفة جديدة" else "➕ إضافة ملاحظة جديدة"
            textSize = 16f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#10B981"))
                cornerRadius = 16f
            }
            setOnClickListener {
                editIndex = -1
                selectedImageUri = null
                currentScreen = "EDITOR"
                render()
            }
        }
        root.addView(addBtn)

        val searchBox = EditText(this).apply {
            hint = "🔎 بحث داخل الملاحظات..."
            setPadding(20, 20, 20, 20)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
        }
        root.addView(searchBox)

        val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val items = load()

        fun updateList(query: String) {
            listContainer.removeAllViews()
            items.forEachIndexed { idx, o ->
                if (o.toString().contains(query, true)) {
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(20, 20, 20, 20)
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor("#F3F4F6"))
                            cornerRadius = 16f
                        }
                    }
                    val cardParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 12, 0, 12) }
                    card.layoutParams = cardParams

                    val itemTitle = TextView(this).apply {
                        text = o.optString("title")
                        textSize = 20f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(Color.parseColor("#1F2937"))
                    }
                    card.addView(itemTitle)

                    val imgUriStr = o.optString("imageUri")
                    if (imgUriStr.isNotEmpty()) {
                        val imgView = ImageView(this).apply {
                            setImageURI(Uri.parse(imgUriStr))
                            adjustViewBounds = true
                            maxHeight = 450
                            setPadding(0, 12, 0, 12)
                        }
                        card.addView(imgView)
                    }

                    val body = TextView(this).apply {
                        text = if (section == "work") {
                            "المكونات:\n${o.optString("ingredients")}\n\nطريقة التحضير:\n${o.optString("body")}"
                        } else {
                            o.optString("body")
                        }
                        textSize = 15f
                        setTextColor(Color.parseColor("#374151"))
                    }
                    card.addView(body)

                    val actions = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, 12, 0, 0)
                    }
                    val editBtn = Button(this).apply {
                        text = "✏️ تعديل"
                        setOnClickListener {
                            editIndex = idx
                            selectedImageUri = o.optString("imageUri", null)
                            currentScreen = "EDITOR"
                            render()
                        }
                    }
                    val delBtn = Button(this).apply {
                        text = "🗑️ حذف"
                        setOnClickListener {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("حذف")
                                .setMessage("هل أنت تأكد من الحذف؟")
                                .setPositiveButton("نعم") { _, _ ->
                                    items.removeAt(idx)
                                    save(items)
                                    render()
                                }
                                .setNegativeButton("إلغاء", null)
                                .show()
                        }
                    }
                    actions.addView(editBtn)
                    actions.addView(delBtn)
                    card.addView(actions)
                    listContainer.addView(card)
                }
            }
        }

        updateList("")
        searchBox.setOnKeyListener { _, _, _ ->
            updateList(searchBox.text.toString())
            false
        }

        root.addView(ScrollView(this).apply { addView(listContainer) })
        setContentView(root)
    }

    // --- 3. صفحة الكتابة والتعديل المستقلة ---
    private fun showEditorScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
        }

        val items = load()
        val old = if (editIndex >= 0 && editIndex < items.size) items[editIndex] else JSONObject()

        val headerText = TextView(this).apply {
            text = if (editIndex < 0) "كتابة ملاحظة جديدة" else "تعديل الملاحظة"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#111827"))
            setPadding(0, 0, 0, 24)
        }
        root.addView(headerText)

        val titleInput = EditText(this).apply {
            hint = "العنوان الرئيسي"
            setText(old.optString("title"))
            setPadding(20, 20, 20, 20)
            setTextColor(Color.BLACK)
        }
        root.addView(titleInput)

        val ingInput = EditText(this).apply {
            hint = "المكونات (للوصفات)"
            setText(old.optString("ingredients"))
            setPadding(20, 20, 20, 20)
            setTextColor(Color.BLACK)
            if (section != "work") visibility = View.GONE
        }
        root.addView(ingInput)

        val bodyInput = EditText(this).apply {
            hint = if (section == "work") "طريقة التحضير التفصيلية" else "أدخل نص الملاحظة هنا..."
            setMinLines(5)
            setText(old.optString("body"))
            setPadding(20, 20, 20, 20)
            setTextColor(Color.BLACK)
        }
        root.addView(bodyInput)

        imagePreview = ImageView(this).apply {
            adjustViewBounds = true
            maxHeight = 350
            if (!selectedImageUri.isNullOrEmpty()) {
                setImageURI(Uri.parse(selectedImageUri))
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
        root.addView(imagePreview)

        val pickImgBtn = Button(this).apply {
            text = "🖼️ إرفاق صورة من المعرض"
            setOnClickListener { imagePicker.launch("image/*") }
        }
        root.addView(pickImgBtn)

        val actionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 0)
        }

        val saveBtn = Button(this).apply {
            text = "💾 حفظ"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2563EB"))
                cornerRadius = 16f
            }
            setOnClickListener {
                val o = JSONObject().apply {
                    put("title", titleInput.text.toString())
                    put("ingredients", ingInput.text.toString())
                    put("body", bodyInput.text.toString())
                    put("imageUri", selectedImageUri ?: "")
                }
                if (editIndex < 0) items.add(0, o) else if (editIndex < items.size) items[editIndex] = o
                save(items)
                currentScreen = "SECTION"
                render()
            }
        }

        val cancelBtn = Button(this).apply {
            text = "❌ إلغاء"
            setOnClickListener {
                currentScreen = "SECTION"
                render()
            }
        }

        actionsLayout.addView(saveBtn)
        actionsLayout.addView(cancelBtn)
        root.addView(actionsLayout)

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun load(): MutableList<JSONObject> {
        val a = JSONArray(prefs.getString(section, "[]"))
        return MutableList(a.length()) { a.getJSONObject(it) }
    }

    private fun save(list: List<JSONObject>) {
        val a = JSONArray()
        list.forEach { a.put(it) }
        prefs.edit().putString(section, a.toString()).apply()
    }
}
