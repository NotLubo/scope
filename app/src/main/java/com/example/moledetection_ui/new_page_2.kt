package com.example.moledetection_ui

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.example.moledetection_ui.db.SnapshotKind
import com.example.moledetection_ui.db.StaticDb
import com.example.moledetection_ui.detection.StaticDetector
import java.io.File
import java.io.IOException
import java.util.*

class new_page_2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_page_2)

        findViewById<Button>(R.id.button_tpRoll).setOnClickListener {
            mSnapMode = SnapshotKind.ROLL
            dispatchTakePictureIntent()
        }
        findViewById<Button>(R.id.button_coin).setOnClickListener {
            mSnapMode = SnapshotKind.COIN
            dispatchTakePictureIntent()
        }

    }

    // todo questionable
    lateinit var mSnapMode: SnapshotKind

    lateinit var mCurrentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = Date().time.toString()
        val storageDir: File? = this.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = absolutePath
            deleteOnExit()
        }
    }


    private fun dispatchTakePictureIntent() {
        // todo correct way to do this is requireContext().contentResolver
        this.applicationContext.also{ context ->
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(context.packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Toast.makeText(context, "Something went wrong.", Toast.LENGTH_LONG).show()
                        null
                    }
                    // Continue only if the File was successfully created
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            context,
                            "com.example.moledetection_ui",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode== REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK){
            StaticDetector.INSTANCE = StaticDetector(
                mCurrentPhotoPath,
                applicationContext,
                mSnapMode)
            // the !! is SO wrong
            if(StaticDetector.INSTANCE!!.MakeSnapshot()){
                val intent = Intent(this, ConfirmPicActivity::class.java).apply {
                    this.putExtra("showOk", true)
                }
                startActivityForResult(intent, REQUEST_CONFIRM)
            } else {
                Toast.makeText(
                    applicationContext,
                    "No " + (if(mSnapMode == SnapshotKind.ROLL) "roll" else "coin") + " or lesion found in photo",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else if(requestCode == REQUEST_CONFIRM && resultCode == Activity.RESULT_OK){
            if(StaticDb.currentLesion?.snapshot != null){
                val intent = Intent(this, new_page_3::class.java).apply {
                    this.putExtra("picPath", mCurrentPhotoPath)
                }
                startActivityForResult(intent, REQUEST_SHOW_COMPARISON)
            } else {
                StaticDb.currentLesion!!.snapshot = StaticDetector.INSTANCE!!.snapshotInstance
                StaticDb.lesions.add(StaticDb.currentLesion!!)
                StaticDb.currentLesion = null
                setResult(Activity.RESULT_OK)
                finish()
            }
        } else if(requestCode == REQUEST_SHOW_COMPARISON && resultCode == Activity.RESULT_OK){
            finish()
        }
    }



    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_CONFIRM = 2
        const val REQUEST_SHOW_COMPARISON = 3
    }
}