# TattooPreview - Kotlin Multiplatform (KMP) & iOS Migration Architecture Plan

## Executive Overview
This document outlines the architectural roadmap for migrating **TattooPreview** to a **Kotlin Multiplatform (KMP)** shared core with native rendering engines for Android (OpenGL ES / CameraX / MediaPipe) and iOS (ARKit / Metal / Vision).

---

## Architectural Abstraction Model

```
 ┌────────────────────────────────────────────────────────────────────────┐
 │                      KMP Shared Core (Kotlin)                          │
 │  - Domain Models (TattooDesign, Artist, TransformState)               │
 │  - Business Logic & ViewModels (moko-mvvm / Decompose)                 │
 │  - Signal Processing (1€ OneEuroFilter algorithm)                      │
 │  - Local Persistence Interface (Room KMP / SQLDelight)                │
 └───────────────────┬────────────────────────────────┬───────────────────┘
                     │ expect                             │ expect
 ┌───────────────────▼──────────────┐   ┌─────────────────▼─────────────────┐
 │        Android Platform          │   │           iOS Platform            │
 │ - CameraX Frame Pipeline         │   │ - AVFoundation Camera Capture     │
 │ - MediaPipe Tasks Vision         │   │ - Apple Vision / ARKit Body Tracking│
 │ - ARCore Augmented Faces         │   │ - ARKit Face Tracking             │
 │ - OpenGL ES 2.0 / 3.0 Shaders    │   │ - Metal Shader Language (MSL)     │
 └──────────────────────────────────┘   └───────────────────────────────────┘
```

---

## Shared vs. Platform-Specific Division

| Layer | Shared (KMP) | Android | iOS |
|---|---|---|---|
| **Data Models** | `TattooDesign`, `Artist`, `SavedPreview` | - | - |
| **Filter Logic** | `OneEuroFilter` (Kotlin) | - | - |
| **Database** | SQLDelight / Room KMP | SQLite | SQLite |
| **Camera Feed** | Interface `CameraProvider` | CameraX | AVFoundation |
| **Landmark Tracking** | Interface `LimbTracker` | MediaPipe Tasks Vision | Apple Vision / ARKit |
| **Face Tracking** | Interface `FaceTracker` | ARCore Augmented Faces | ARKit `ARFaceTrackingConfiguration` |
| **Real-Time Rendering**| Uniform state & mesh grid math | OpenGL ES / Compose Canvas | Metal Engine / SwiftUI |

---

## iOS Implementation Strategy

### 1. Camera & AR Tracking
- **AVFoundation**: Low-latency camera capture session emitting `CVPixelBuffer` frames.
- **ARKit (`ARFaceTrackingConfiguration`)**: Tracks 3D face mesh topology (1,220 vertices) for face and neck tattoo placement.
- **Apple Vision Framework (`VNDetectHumanHandPoseRequest`, `VNDetectHumanBodyPoseRequest`)**: High-precision 2D/3D hand, wrist, arm, and torso pose landmarks.

### 2. Metal Shader Blending & Curved Limb Deformer
- **Metal Shaders (`.metal`)**:
  - `vertex`: Deforms quad vertices using cylindrical limb curvature formulas.
  - `fragment`: Performs multiply blending ($C_{blended} = C_{skin} \times C_{tattoo}$), ink aging, and segmentation mask alpha clipping.

```metal
#include <metal_stdlib>
using namespace metal;

struct VertexIn {
    float4 position [[attribute(0)]];
    float2 texCoords [[attribute(1)]];
};

struct VertexOut {
    float4 position [[position]];
    float2 texCoords;
};

vertex VertexOut tattooVertex(VertexIn in [[stage_in]],
                             constant float4x4 &mvpMatrix [[buffer(1)]]) {
    VertexOut out;
    out.position = mvpMatrix * in.position;
    out.texCoords = in.texCoords;
    return out;
}

fragment float4 tattooFragment(VertexOut in [[stage_in]],
                              texture2d<float> tattooTexture [[texture(0)]],
                              sampler textureSampler [[sampler(0)]]) {
    float4 color = tattooTexture.sample(textureSampler, in.texCoords);
    if (color.a < 0.02) discard_fragment();
    return float4(color.rgb, color.a * 0.9);
}
```

---

## Migration Steps Timeline
1. **Phase 1**: Extract `OneEuroFilter`, domain models, and transform calculations into a `:shared` KMP module (`commonMain`).
2. **Phase 2**: Replace Android Room with SQLDelight / Room KMP for cross-platform local SQLite database access.
3. **Phase 3**: Implement iOS target in Xcode with SwiftUI, Metal Shader renderer, and ARKit body tracking.
