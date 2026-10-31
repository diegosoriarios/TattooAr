package com.diego.tattooar.ui.render

object ShaderCode {

    val VERTEX_SHADER = """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        
        uniform mat4 uMVPMatrix;
        uniform float uCylindricalCurvature; // 0.0 = flat, 1.0 = full cylinder wrap
        
        varying vec2 vTexCoord;
        
        void main() {
            vec4 pos = aPosition;
            
            if (uCylindricalCurvature > 0.01) {
                float angle = pos.x * uCylindricalCurvature * 1.2;
                pos.x = sin(angle);
                pos.z = (1.0 - cos(angle)) * 0.4;
            }
            
            gl_Position = uMVPMatrix * pos;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    val FRAGMENT_SHADER = """
        precision mediump float;
        
        varying vec2 vTexCoord;
        
        uniform sampler2D uTattooTexture;
        uniform sampler2D uMaskTexture;     // Person segmentation occlusion mask
        uniform float uUseOcclusionMask;   // 0.0 or 1.0
        uniform float uOpacity;            // 0.1 to 1.0
        uniform float uInkFade;            // 0.0 to 1.0 (0 = fresh ink, 1 = aged/faded ink)
        uniform float uIsMirrored;         // 0.0 or 1.0
        
        void main() {
            vec2 texCoord = vTexCoord;
            if (uIsMirrored > 0.5) {
                texCoord.x = 1.0 - texCoord.x;
            }
            
            vec4 tattooColor = texture2D(uTattooTexture, texCoord);
            
            if (tattooColor.a < 0.02) {
                discard;
            }
            
            float maskAlpha = 1.0;
            if (uUseOcclusionMask > 0.5) {
                maskAlpha = texture2D(uMaskTexture, vTexCoord).a;
            }
            
            if (maskAlpha < 0.05) {
                discard;
            }
            
            // Aged ink color shift
            vec3 agedTint = vec3(0.15, 0.22, 0.26);
            vec3 fadedInk = mix(tattooColor.rgb, clamp(tattooColor.rgb + agedTint * uInkFade, 0.0, 1.0), uInkFade * 0.55);
            
            fadedInk = mix(fadedInk, vec3(0.85, 0.85, 0.85), uInkFade * 0.35);
            
            float effectiveAlpha = tattooColor.a * uOpacity * maskAlpha * mix(1.0, 0.65, uInkFade);
            
            gl_FragColor = vec4(fadedInk, effectiveAlpha);
        }
    """.trimIndent()
}
