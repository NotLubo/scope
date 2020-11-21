package com.example.moledetection_ui.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import com.example.moledetection_ui.db.Snapshot
import com.example.moledetection_ui.db.SnapshotKind

class StaticDetector(picUri: String, context: Context, val mMode: SnapshotKind) {

    private val mClassifierMoles: ImageClassifierNew
    private val mClassifierCoins: ImageClassifierNew
    private val mClassifierDefects: ImageClassifierNew
    val mCropBitmap: Bitmap

    init {
        val source = BitmapFactory.decodeFile(picUri)
        val targetSize = if (source.width >= source.height) source.height else source.width
        mCropBitmap = ThumbnailUtils.extractThumbnail(source, targetSize, targetSize)

        mClassifierDefects = ImageClassifierNew(context, "model_new/model.tflite", arrayOf("Defect", "Roll"), arrayOf(0.8f, 0.8f))
        mClassifierCoins = ImageClassifierNew(context, "model_coins/model.tflite", arrayOf("Coin"), arrayOf(0.8f))
        mClassifierMoles = ImageClassifierNew(context, "model_moles/model.tflite", arrayOf("Roll", "Naevus"), arrayOf(0.9f, 0.3f))
    }

    public fun MakeSnapshot() : Boolean {
        snapshotInstance = RunPredict()
        return snapshotInstance != null
    }

    private fun RunPredict() : Snapshot? {
        val scaled = Bitmap.createScaledBitmap(
            mCropBitmap,
            ImageClassifierNew.INPUT_WIDTH,
            ImageClassifierNew.INPUT_WIDTH,
            false
        )

        var predictions: Iterable<Prediction> =
            mClassifierDefects.detect(scaled) + mClassifierMoles.detect(scaled)
        val measureBox =
            when (mMode) {
                SnapshotKind.COIN -> {
                    predictions += mClassifierCoins.detect(scaled)
                    findBest("Coin", predictions)?.bbox
                }
                SnapshotKind.ROLL -> {
                    findBest("Roll", predictions)?.bbox
                }
                else -> null
            } ?: return null

        val resultBox = findBest(hashSetOf("Defect", "Naevus"), predictions)?.bbox ?: return null

        return Snapshot(
            0,
            mCropBitmap,
            mMode,
            measureBox,
            resultBox
            )
    }

    public var snapshotInstance : Snapshot? = null

    companion object {
        // I sense a disturbance in the force
        // as if a million devs have silently wept
        public var INSTANCE: StaticDetector? = null

        private fun findBest(categories: HashSet<String>, predictions: Iterable<Prediction>): Prediction?{
            return predictions.filter {pred ->
                 categories.contains(pred.label)
            }.maxOrNull()
        }

        private fun findBest(category: String, predictions: Iterable<Prediction>): Prediction?{
            return predictions.filter {pred ->
                category == pred.label
            }.maxOrNull()
        }
    }
}