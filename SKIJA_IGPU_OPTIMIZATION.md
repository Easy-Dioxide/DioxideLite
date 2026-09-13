# DioxideLite v2.0.1 — Skija iGPU Pipeline Optimization

This pass focuses on reducing unnecessary Skija frame submission overhead while
preserving the existing Dynamic Island animation timing and visual effects.

## Preserved

- Dynamic Island animation remains frame-driven.
- Existing easing/expand/collapse timing is unchanged.
- Glow, outline, gradient and highlight layers are not removed.
- HUD visual behavior is not intentionally downgraded.

## Optimization strategy

1. Add a frame-work fast path so an empty Skija frame can be skipped.
2. Keep animation state updates independent from the rendering submission gate.
3. Avoid introducing a global FPS cap: Minecraft can continue rendering at its
   configured frame rate.
4. Keep the optimization isolated in SkijaRenderer so visual modules do not
   need to know about GPU capability.

## Validation note

A full Gradle compilation could not be completed in the current environment
because the Gradle wrapper requires downloading Gradle 9.2.1 and the environment
cannot reach services.gradle.org.
