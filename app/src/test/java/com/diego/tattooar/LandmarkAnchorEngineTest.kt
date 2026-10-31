package com.diego.tattooar

import com.diego.tattooar.domain.tracking.LandmarkAnchorEngine
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LandmarkAnchorEngineTest {

    @Test
    fun testEmptyLandmarksReturnsUntrackedAnchor() {
        val engine = LandmarkAnchorEngine()
        val transform = engine.computeAnchor(null, 1080f, 1920f, System.currentTimeMillis())
        assertFalse(transform.isTracked)
    }

    @Test
    fun testValidLandmarksComputesAnchor() {
        val engine = LandmarkAnchorEngine()
        val dummyLandmarks = MutableList(21) { idx ->
            NormalizedLandmark.create(0.5f, 0.5f + (idx * 0.01f), 0f)
        }

        val transform = engine.computeAnchor(dummyLandmarks, 1080f, 1920f, System.currentTimeMillis())
        assertTrue("Transform should be tracked", transform.isTracked)
        assertTrue("Scale should be positive", transform.scale > 0f)
    }
}
