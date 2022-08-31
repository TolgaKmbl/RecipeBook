package com.tolgakumbul.recipebook

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_detail.*
import java.io.ByteArrayOutputStream

class DetailFragment : Fragment() {

    var pickedImage: Uri? = null;
    var pickedBitmap: Bitmap? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addImageLogo.setOnClickListener {
            addImage(it)
        }
        saveRecipeButton.setOnClickListener {
            saveRecipe(it)
        }
        arguments?.let {
            if ("fromList".equals(DetailFragmentArgs.fromBundle(it).fromInfo)) {
                saveRecipeButton.visibility = View.INVISIBLE
                val recipeId = DetailFragmentArgs.fromBundle(it).idInfo
                try {
                    context?.let {
                        val database =
                            it.openOrCreateDatabase("RecipeDB", Context.MODE_PRIVATE, null)
                        val cursor = database.rawQuery(
                            "SELECT * FROM recipes WHERE id = ?",
                            arrayOf(recipeId.toString())
                        )
                        val recipeNameIndex = cursor.getColumnIndex("name")
                        val recipeIngredientsIndex = cursor.getColumnIndex("ingredients")
                        val recipePicIndex = cursor.getColumnIndex("pic")
                        while (cursor.moveToNext()) {
                            recipeName.setText(cursor.getString(recipeNameIndex))
                            ingredients.setText(cursor.getString(recipeIngredientsIndex))
                            val byteArray = cursor.getBlob(recipePicIndex)
                            addImageLogo.setImageBitmap(
                                BitmapFactory.decodeByteArray(
                                    byteArray,
                                    0,
                                    byteArray.size
                                )
                            )
                        }
                        cursor.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun addImage(view: View) {
        activity?.let {
            if (ContextCompat.checkSelfPermission(
                    it.applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //Get permission
                requestPermissions(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    Constants.REQUEST_CODE_ONE
                )
            } else {
                //Permission granted already
                val galleryIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, Constants.REQUEST_CODE_TWO)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_CODE_ONE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val galleryIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, Constants.REQUEST_CODE_TWO)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_TWO && resultCode == Activity.RESULT_OK && data != null) {
            pickedImage = data.data
            try {
                context?.let {
                    if (pickedImage != null) {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source =
                                ImageDecoder.createSource(it.contentResolver, pickedImage!!)
                            pickedBitmap = ImageDecoder.decodeBitmap(source)
                            addImageLogo.setImageBitmap(pickedBitmap)
                        } else {
                            pickedBitmap =
                                MediaStore.Images.Media.getBitmap(it.contentResolver, pickedImage)
                            addImageLogo.setImageBitmap(pickedBitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveRecipe(view: View) {
        val recipeName = recipeName.text.toString()
        val ingredients = ingredients.text.toString()
        if (pickedBitmap != null) {
            val recipeImage = resizeBitmap(pickedBitmap!!, 300)
            val outputStream = ByteArrayOutputStream()
            recipeImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                context?.let {
                    val database = it.openOrCreateDatabase("RecipeDB", Context.MODE_PRIVATE, null)
                    database.execSQL("CREATE TABLE IF NOT EXISTS recipes(id INTEGER PRIMARY KEY, name VARCHAR, ingredients VARCHAR, pic BLOB)")
                    val sqlString = "INSERT INTO recipes (name, ingredients, pic) VALUES (?,?,?)"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1, recipeName)
                    statement.bindString(2, ingredients)
                    statement.bindBlob(3, byteArray)
                    statement.execute()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val action = DetailFragmentDirections.actionDetailFragmentToListFragment()
            Navigation.findNavController(view).navigate(action)
        }

    }

    private fun resizeBitmap(selectedBitmap: Bitmap, maxSize: Int): Bitmap {
        var width = selectedBitmap.width
        var height = selectedBitmap.height

        val bitmapScale: Double = width.toDouble() / height.toDouble()

        if (bitmapScale > 1) {
            // horizontal bitmap
            width = maxSize
            val croppedHeight = width / bitmapScale
            height = croppedHeight.toInt()
        } else {
            // vertical bitmap
            height = maxSize
            val croppedWidth = height * bitmapScale
            width = croppedWidth.toInt()
        }
        return Bitmap.createScaledBitmap(selectedBitmap, width, height, true)
    }

}