package com.mothakrati.app
import android.app.*
import android.os.*
import android.graphics.Color
import android.view.*
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity: Activity() {
    private val prefs by lazy { getSharedPreferences("mothakrati",0) }
    private var section="german"
    private val titles=mapOf("german" to "🇩🇪 الألمانية","programming" to "🐍 البرمجة","work" to "👨‍🍳 العمل / الوصفات","ideas" to "💡 أفكاري")
    override fun onCreate(b:Bundle?){super.onCreate(b); show()}
    private fun show(){
        val root=LinearLayout(this); root.orientation=LinearLayout.VERTICAL; root.setPadding(18,18,18,18)
        val head=TextView(this); head.text="📚 مذكرتي\nكل اللي اتعلمته وعملته في مكان واحد"; head.textSize=22f; head.setTextColor(Color.WHITE); head.setPadding(16,16,16,16); head.setBackgroundColor(Color.rgb(25,25,25)); root.addView(head)
        val nav=HorizontalScrollView(this); val row=LinearLayout(this); 
        titles.forEach{(k,v)-> val x=Button(this);x.text=v;x.setOnClickListener{section=k;show()};row.addView(x)}
        nav.addView(row);root.addView(nav)
        val add=Button(this); add.text=if(section=="work")"➕ إضافة وصفة" else "➕ إضافة ملاحظة"; add.setOnClickListener{editor(-1)};root.addView(add)
        val list=LinearLayout(this);list.orientation=LinearLayout.VERTICAL
        val q=EditText(this);q.hint="🔎 بحث";root.addView(q); 
        val items=load().filter{it.toString().contains(q.text.toString(),true)}
        items.forEachIndexed{idx,o->
            val card=LinearLayout(this);card.orientation=LinearLayout.VERTICAL;card.setPadding(12,12,12,12)
            val t=TextView(this);t.text=o.optString("title");t.textSize=19f;card.addView(t)
            val body=TextView(this);body.text=if(section=="work")"المكونات:\n${o.optString("ingredients")}\n\nطريقة التحضير:\n${o.optString("body")}" else o.optString("body");card.addView(body)
            val actions=LinearLayout(this);val e=Button(this);e.text="✏️ تعديل";e.setOnClickListener{editor(idx)}
            val d=Button(this);d.text="🗑️ حذف";d.setOnClickListener{val a=load();a.remove(idx);save(a);show()}
            actions.addView(e);actions.addView(d);card.addView(actions);list.addView(card)
        }
        root.addView(ScrollView(this).apply{addView(list)})
        setContentView(root)
    }
    private fun load():MutableList<JSONObject>{val a=JSONArray(prefs.getString(section,"[]"));return MutableList(a.length()){a.getJSONObject(it)}}
    private fun save(list:List<JSONObject>){val a=JSONArray();list.forEach{a.put(it)};prefs.edit().putString(section,a.toString()).apply()}
    private fun editor(index:Int){
        val a=load();val old=if(index>=0)a[index] else JSONObject()
        val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;box.setPadding(24,10,24,10)
        val title=EditText(this);title.hint="العنوان";title.setText(old.optString("title"));box.addView(title)
        val ing=EditText(this);ing.hint="المكونات (للوصفات)";ing.setText(old.optString("ingredients")); if(section!="work")ing.visibility=View.GONE;box.addView(ing)
        val body=EditText(this);body.hint=if(section=="work")"طريقة التحضير" else "ماذا استفدت؟ / الملاحظات";body.setMinLines(6);body.setText(old.optString("body"));box.addView(body)
        AlertDialog.Builder(this).setTitle(if(index<0)"إضافة" else "تعديل").setView(box).setPositiveButton("حفظ"){_,_->
            val o=JSONObject();o.put("title",title.text.toString());o.put("ingredients",ing.text.toString());o.put("body",body.text.toString())
            if(index<0)a.add(0,o) else a[index]=o;save(a);show()
        }.setNegativeButton("إلغاء",null).show()
    }
}
