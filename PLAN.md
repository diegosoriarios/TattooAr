# TattooPreview - Architecture & Implementation Plan

## Executive Summary
**TattooPreview** is a native Android application designed to provide realistic, real-time preview of tattoo designs on skin via live camera feed. Built with Kotlin, Jetpack Compose, CameraX, MediaPipe, OpenGL ES, and ARCore.

---

## Technical Architecture Overview

```
                          ┌─────────────────────────┐
                          │     MainActivity        │
                          └────────────┬────────────┘
                                       │
                          ┌────────────▼────────────┐
                          │       MainScreen        │
                          └────────────┬────────────┘
                                       │
          ┌────────────────────────────┼────────────────────────────┐
          │                            │                            │
┌─────────▼─────────┐        ┌─────────▼─────────┐        ┌─────────▼─────────┐
│   CameraPreview   │        │ TattooOverlayView │        │ ControlsOverlay   │
│   (CameraX)       │        │ (Canvas / GLES)   │        │ (Compose M3)      │
└─────────┬─────────┘        └─────────▲─────────┘        └─────────▲─────────┘
          │                            │                            │
┌─────────▼────────────────────────────┴─────────┐        ┌─────────▼─────────┐
│           AR & Computer Vision Engine          │        │  Room DB / Repo   │
│  - HandLandmarkerHelper (Hands/Forearm)        │        │ - Tattoo Catalog  │
│  - PoseLandmarkerHelper (Arms, Legs, Torso)    │        │ - Favorites       │
│  - ARCoreFaceTracker (Face & Neck)             │        │ - Saved Previews  │
│  - CylindricalMeshDeformer (Curved Limbs)      │        │ - Artist Directory│
│  - SegmentationOcclusionHelper (Clothes/Hair)  │        └───────────────────┘
└────────────────────────────────────────────────┘
```

---

## Phased Roadmap

### Phase 1 – MVP (2D Overlay, No Tracking) [COMPLETED]
- Camera preview screen with CameraX & runtime permission handling.
- Photo Picker API integration + bundled sample designs.
- Luminance white-to-transparent background removal processor.
- Interactive pan, pinch-to-scale, two-finger rotation, and mirror flip gestures.
- Real-time controls for Opacity ($0.1 - 1.0$) and Ink Aging / Fade.
- GLSL Multiply blend shader (`C_result = C_skin * C_tattoo`).
- Snapshot capture & device MediaStore saving / sharing.
- Persistent preview disclaimer screen on first launch.

### Phase 2 – v1: Hand & Forearm Tracking [COMPLETED]
- MediaPipe Hand Landmarker on CameraX `ImageAnalysis` stream.
- Landmark Anchoring Engine (local coordinate system relative to wrist, palm, forearm).
- One Euro Filter jitter reduction.
- Smooth tracking loss fade-out & re-acquisition.
- Skin tone & ambient lighting sampling.

### Phase 3 – v2: Advanced Realism [COMPLETED]
- ARCore Augmented Faces face and neck region anchor tracking.
- MediaPipe Pose Landmarker body tracking for arms, legs, shoulders, and torso.
- Cylindrical Limb Mesh Deformer for 3D curved-limb wrapping.
- Person Segmentation occlusion mask so tattoo stays on skin only.

### Phase 4 – v3: Product Features [COMPLETED]
- **Tattoo Library & Search**: Categorized tattoo library with live search, categories, and favorites.
- **Room Database Persistence**: Local SQLite database storing tattoo designs and saved preview snapshots.
- **Artist Directory & Booking**: Artist profiles, portfolios, and "Book an Artist" consultation request flow.
- **Kotlin Multiplatform (KMP) Architecture Plan**: Architectural roadmap for iOS porting with ARKit & Metal ([KMP_MIGRATION_PLAN.md](file:///home/diego/Documentos/Personal/TattooAr/KMP_MIGRATION_PLAN.md)).

---

## Phase 4 Deliverables & Checklist
- [x] Room Entity & DAO definitions (`TattooEntity.kt`, `SavedPreviewEntity.kt`, `TattooDao.kt`, `AppDatabase.kt`)
- [x] Room Repository implementation (`TattooRepository.kt`)
- [x] Tattoo Library UI with Search & Favorites (`TattooCatalogBottomSheet.kt`)
- [x] Artist Directory & Booking Flow (`Artist.kt`, `ArtistBookingDialog.kt`)
- [x] Saved Preview History Gallery
- [x] KMP / iOS ARKit migration plan (`KMP_MIGRATION_PLAN.md`)
- [x] Build & verification test
