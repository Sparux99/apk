package com.amine.player

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // مؤقتًا نضع layout فارغ فقط لتجنب الأخطاء
        setContentView(android.R.layout.simple_list_item_1)
    }
}
