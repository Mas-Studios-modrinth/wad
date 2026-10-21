# RunParticle -> Minecraft 26.3 (Fabric)

Target: MC 26.3, Fabric Loader 0.19.5+, Fabric API 0.161.0+26.3, Loom 1.17, Gradle 9.6.0, Java 25.

## Changes made (against the 26.2 -> 26.3 primer)
- Project layout: sources moved to src/main/java, assets/mod json to src/main/resources (the old zip was not buildable as laid out).
- PoseStack.mulPose -> rotate (ActionsStuffXpOrbRenderer, ActionsStuffThrowableRenderer).
- RenderTypes.entityTranslucentEmissive(Identifier, boolean) was removed -> single-arg overload (ActionsStuffXpOrbShaders.glow).
- OrderedSubmitNodeCollector.submitModel no longer takes a CrumblingOverlay -> dropped trailing null (ActionsStuffHurtOverlayFeatureRenderer).
- InputConstants.Type.KEYSYM -> KEYBOARD; unbound key now InputConstants.UNKNOWN (SDL input rewrite).
- ItemInHandRenderer -> FirstPersonHandsAndItemsRenderer in FirstPersonToolTrailMixin.
- xp_orb.vsh/.fsh: #moj_import -> #include, GL_ARB_separate_shader_objects, explicit layout(location) on all in/out.
- build.gradle: Java 25 release, libs/ folder for Actions & Stuff jars; Gradle wrapper pinned to 9.6.0.

## NOT verified (no network / Java 21 / no MC 26.3 jars in the sandbox - nothing was compiled)
- Method/signature guesses: PoseStack.rotate(Quaternionfc), submitModel 7-arg overload, entityTranslucentEmissive(Identifier), InputConstants.UNKNOWN.
- Mixin targets that the primer doesn't mention but may have moved: ItemStackRenderState.submit(PoseStack,SubmitNodeCollector,III), ItemInHandLayer.submitArmWithItem, LivingEntityRenderer.submit/getOverlayCoords, ExperienceOrbRenderer.submit/extractRenderState.
- FirstPersonToolTrailMixin hooks dev.aleca.actionsstufffp, whose own 26.3 signature I can't see.
- The A&S libs are not in the archive; ActionsStuffThrowableRenderer won't compile without them in libs/.
First step on your machine: ./gradlew build, then fix whatever javac / mixin apply reports.

## Building
- Local: `./build.sh` (Linux/macOS) or `build.bat` (Windows), JDK 25 required. Jar lands in `dist/`.
- CI: push to GitHub; `.github/workflows/build.yml` builds with JDK 25 + Gradle 9.6.0 and uploads the jar as an artifact.
- Put the Actions & Stuff jars in `libs/` first (commit them if using CI).
