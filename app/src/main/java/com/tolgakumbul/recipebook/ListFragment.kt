package com.tolgakumbul.recipebook

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_list.*

class ListFragment : Fragment() {

    var recipeNameList = ArrayList<String>()
    var recipeIdList = ArrayList<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addRecipeButton.setOnClickListener {
            val action = ListFragmentDirections.actionListFragmentToDetailFragment()
            Navigation.findNavController(it).navigate(action)
        }
        sqlDataFetch()
    }

    private fun sqlDataFetch() {
        try {
            context?.let {
                val database = it.openOrCreateDatabase("RecipeDB", Context.MODE_PRIVATE, null)
                val cursor = database.rawQuery("SELECT * FROM recipes", null)
                val recipeNameIndex = cursor.getColumnIndex("name")
                // The recipes might share a common name, id will be the distinction
                val idIndex = cursor.getColumnIndex("id")
                recipeNameList.clear()
                recipeIdList.clear()
                while (cursor.moveToNext()) {
                    recipeNameList.add(cursor.getString(recipeNameIndex))
                    recipeIdList.add(cursor.getInt(idIndex))
                }
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}