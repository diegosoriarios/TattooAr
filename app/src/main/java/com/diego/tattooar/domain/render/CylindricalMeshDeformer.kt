package com.diego.tattooar.domain.render

import kotlin.math.cos
import kotlin.math.sin

data class DeformedMeshGrid(
    val vertices: FloatArray,    // [x, y, z] per vertex
    val texCoords: FloatArray,   // [u, v] per vertex
    val indices: ShortArray,     // Triangle element indices
    val gridWidth: Int,
    val gridHeight: Int
)

object CylindricalMeshDeformer {

    /**
     * Generates a 3D cylindrical mesh grid for realistic curved limb tattoo wrapping.
     * @param gridCols Number of columns (e.g. 16)
     * @param gridRows Number of rows (e.g. 16)
     * @param curvatureIntensity Curvature wrapping factor (0.0 = flat, 1.0 = strong cylindrical curve)
     */
    fun generateCylindricalMesh(
        gridCols: Int = 16,
        gridRows: Int = 16,
        curvatureIntensity: Float = 0.85f
    ): DeformedMeshGrid {
        val numVertices = (gridCols + 1) * (gridRows + 1)
        val vertices = FloatArray(numVertices * 3)
        val texCoords = FloatArray(numVertices * 2)

        var vertexIdx = 0
        var texIdx = 0

        val arcMax = Math.toRadians(80.0 * curvatureIntensity).toFloat()

        for (r in 0..gridRows) {
            val v = r.toFloat() / gridRows.toFloat()
            val y = 1.0f - 2.0f * v

            for (c in 0..gridCols) {
                val u = c.toFloat() / gridCols.toFloat()
                val xNorm = 2.0f * u - 1.0f

                // Cylindrical projection around Y axis
                val theta = xNorm * arcMax
                val x = sin(theta)
                val z = (1.0f - cos(theta)) * 0.5f

                vertices[vertexIdx++] = x
                vertices[vertexIdx++] = y
                vertices[vertexIdx++] = z

                texCoords[texIdx++] = u
                texCoords[texIdx++] = v
            }
        }

        // Generate triangle indices
        val numTriangles = gridCols * gridRows * 2
        val indices = ShortArray(numTriangles * 3)
        var indexIdx = 0

        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                val topLeft = (r * (gridCols + 1) + c).toShort()
                val topRight = (topLeft + 1).toShort()
                val bottomLeft = ((r + 1) * (gridCols + 1) + c).toShort()
                val bottomRight = (bottomLeft + 1).toShort()

                // Triangle 1
                indices[indexIdx++] = topLeft
                indices[indexIdx++] = bottomLeft
                indices[indexIdx++] = topRight

                // Triangle 2
                indices[indexIdx++] = topRight
                indices[indexIdx++] = bottomLeft
                indices[indexIdx++] = bottomRight
            }
        }

        return DeformedMeshGrid(
            vertices = vertices,
            texCoords = texCoords,
            indices = indices,
            gridWidth = gridCols,
            gridHeight = gridRows
        )
    }
}
