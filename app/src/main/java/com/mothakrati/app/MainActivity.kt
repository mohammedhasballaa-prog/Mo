package com.mothakrati.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
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
    private var section = "german"
    private var selectedImageUri: String? = null
    private var imagePreviewInDialog: ImageView? = null

    private val titles = mapOf(
        "german" to "🇩🇪 الألمانية",
        "programming" to "🐍 البرمجة",
        "work" to "👨‍🍳 العمل / الوصفات",
        "ideas" to "💡 أفكاري"
    )

    // أداة اختيار صور الملاحظات
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedImageUri = it.toString()
            imagePreviewInDialog?.setImageURI(it)
            imagePreviewInDialog?.visibility = View.VISIBLE
        }
    }

    // أداة اختيار أيقونة البرنامج
    private val appIconPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            prefs.edit().putString("custom_app_icon", it.toString()).apply()
            show()
        }
    }

    // أداة اختيار خلفية التطبيق
    private val bgPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            prefs.edit().putString("custom_bg", it.toString()).apply()
            show()
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        show()
    }

    private fun show() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            
            // تطبيق الخلفية المختارة إذا وجدت
            val bgUri = prefs.getString("custom_bg", null)
            if (!bgUri.isNullOrEmpty()) {
                try {
                    val bgView = ImageView(this@MainActivity).apply {
                        setImageURI(Uri.parse(bgUri))
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    // ملاحظة: لو حابب تخليه خلفية بسيطة، نقدر نعتمد على اللون أو نضع الصورة كخلفية رئيسية
                } catch (e: Exception) {}
            }
            setBackgroundColor(Color.rgb(20, 20, 20))
        }

        // رأس التطبيق مع الأيقونة المخصصة
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.rgb(35, 35, 35))
                cornerRadius = 16f
            }
        }

        // عرض الأيقونة (إما المخصصة أو الحرف الافتراضي R)
        val customIconUri = prefs.getString("custom_app_icon", null)
        if (!customIconUri.isNullOrEmpty()) {
            val iconImg = ImageView(this).apply {
                setImageURI(Uri.parse(customIconUri))
                layoutParams = LinearLayout.LayoutParams(110, 110).apply { setMargins(0, 0, 16, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            headerLayout.addView(iconImg)
        } else {
            val appIcon = TextView(this).apply {
                text = "R"
                textSize = 22f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.rgb(103, 58, 183))
                }
                layoutParams = LinearLayout.LayoutParams(100, 100).apply { setMargins(0, 0, 16, 0) }
            }
            headerLayout.addView(appIcon)
        }

        val textLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val headText = TextView(this).apply {
            text = "rooh - تطبيق روح"
            textSize = 18f
            setTextColor(Color.WHITE)
        }
        textLayout.addView(headText)
        headerLayout.addView(textLayout)

        // زر تغيير الأيقونة والخلفية من داخل الواجهة
        val settingsLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val changeIconBtn = Button(this).apply {
            text = "🖼️ الأيقونة"
            textSize = 11f
            setOnClickListener { appIconPicker.launch("image/*") }
        }
        val changeBgBtn = Button(this).apply {
            text = "🎨 الخلفية"
            textSize = 11f
            setOnClickListener { bgPicker.launch("image/*") }
        }
        settingsLayout.addView(changeIconBtn)
        settingsLayout.addView(changeBgBtn)
        headerLayout.addView(settingsLayout)

        root.addView(headerLayout)

        val nav = HorizontalScrollView(this)
        val row = LinearLayout(this)
        titles.forEach { (k, v) ->
            val x = Button(this).apply {
                text = v
                setOnClickListener {
                    section = k
                    show()
                }
            }
            row.addView(x)
        }
        nav.addView(row)
        root.addView(nav)

        val add = Button(this).apply {
            text = if (section == "work") "➕ إضافة وصفة" else "➕ إضافة ملاحظة"
            setOnClickListener { editor(-1) }
        }
        root.addView(add)

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val q = EditText(this).apply { hint = "🔎 بحث في الملاحظات..." }
        root.addView(q)

        val items = load()
        items.forEachIndexed { idx, o ->
            if (o.toString().contains(q.text.toString(), true)) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 16, 16, 16)
                    background = GradientDrawable().apply {
                        setColor(Color.rgb(245, 245, 245))
                        cornerRadius = 12f
                    }
                }

                val cardParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 10, 0, 10) }
                card.layoutParams = cardParams

                val t = TextView(this).apply {
                    text = o.optString("title")
                    textSize = 19f
                    setTextColor(Color.BLACK)
                }
                card.addView(t)

                val imgUriStr = o.optString("imageUri")
                if (imgUriStr.isNotEmpty()) {
                    val imgView = ImageView(this).apply {
                        setImageURI(Uri.parse(imgUriStr))
                        adjustViewBounds = true
                        maxHeight = 400
                        setPadding(0, 8, 0, 8)
                    }
                    card.addView(imgView)
                }

                val body = TextView(this).apply {
                    text = if (section == "work") {
                        "المكونات:\n${o.optString("ingredients")}\n\nطريقة التحضير:\n${o.optString("body")}"
                    } else {
                        o.optString("body")
                    }
                    setTextColor(Color.DKGRAY)
                }
                card.addView(body)

                val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                val e = Button(this).apply {
                    text = "✏️ تعديل"
                    setOnClickListener { editor(idx) }
                }
                val d = Button(this).apply {
                    text = "🗑️ حذف"
                    setOnClickListener {
                        val currentList = load()
                        if (idx < currentList.size) {
                            currentList.removeAt(idx)
                            save(currentList)
                            show()
                        }
                    }
                }
                actions.addView(e)
                actions.addView(d)
                card.addView(actions)
                list.addView(card)
            }
        }

        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)
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

    private fun editor(index: Int) {
        val a = load()
        val old = if (index >= 0 && index < a.size) a[index] else JSONObject()
        selectedImageUri = old.optString("imageUri", null)

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 10, 24, 10)
        }

        val title = EditText(this).apply {
            hint = "العنوان"
            setText(old.optString("title"))
        }
        box.addView(title)

        val ing = EditText(this).apply {
            hint = "المكونات (للوصفات)"
            setText(old.optString("ingredients"))
            if (section != "work") visibility = View.GONE
        }
        box.addView(ing)

        val body = EditText(this).apply {
            hint = if (section == "work") "طريقة التحضير" else "ماذا استفدت؟ / الملاحظات"
            setMinLines(4)
            setText(old.optString("body"))
        }
        box.addView(body)

        imagePreviewInDialog = ImageView(this).apply {
            adjustViewBounds = true
            maxHeight = 300
            if (!selectedImageUri.isNullOrEmpty()) {
                setImageURI(Uri.parse(selectedImageUri))
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
        box.addView(imagePreviewInDialog)

        val pickImgBtn = Button(this).apply {
            text = "🖼️ إرفاق صورة بالملاحظة"
            setOnClickListener { imagePicker.launch("image/*") }
        }
        box.addView(pickImgBtn)

        AlertDialog.Builder(this)
            .setTitle(if (index < 0) "إضافة ملاحظة جديدة" else "تعديل الملاحظة")
            .setView(box)
            .setPositiveButton("حفظ") { _, _ ->
                val o = JSONObject().apply {
                    put("title", title.text.toString())
                    put("ingredients", ing.text.toString())
                    put("body", body.text.toString())
                    put("imageUri", selectedImageUri ?: "")
                }

                if (index < 0) a.add(0, o) else if (index < a.size) a[index] = o
                save(a)
                show()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
