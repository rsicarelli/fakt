// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import java.util.concurrent.Callable
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * One compilation of a target, reduced to the source set names the ownership rules need.
 *
 * @property default The compilation's default source set.
 * @property members Every source set the compilation lists directly (KGP `kotlinSourceSets`),
 *   default included.
 * @property isTest True for a test-like compilation.
 */
internal data class CompilationFacts(
    val default: String,
    val members: List<String>,
    val isTest: Boolean,
)

/**
 * The source sets of the non-test compilations of one target.
 *
 * @property defaults The default source set of each compilation.
 * @property members Every other source set those compilations list directly, in first seen order.
 */
internal data class VariantSourceSets(val defaults: List<String>, val members: List<String>)

/**
 * Splits the non-test compilations into their default source sets and the members they list next to
 * them, for instance `androidMain` next to `androidDebug`. Test compilations are ignored.
 */
internal fun variantSourceSets(compilations: List<CompilationFacts>): VariantSourceSets {
    val mains = compilations.filterNot { it.isTest }
    val defaults = mains.map { it.default }
    return VariantSourceSets(
        defaults = defaults,
        members = mains.flatMap { it.members }.filterNot { it in defaults }.distinct(),
    )
}

/**
 * Builds the [TargetNode] of one target from its compilations. The main source sets are the
 * defaults of the non-test compilations (the target's own `<name>Main` when there is none).
 */
internal fun targetNodeOf(
    name: String,
    platformType: String,
    isAndroid: Boolean,
    compilations: List<CompilationFacts>,
): TargetNode {
    val sets = variantSourceSets(compilations)
    return TargetNode(
        name = name,
        platformType = platformType,
        isAndroid = isAndroid,
        mainSourceSets = sets.defaults.ifEmpty { listOf("${name}Main") },
        memberSourceSets = sets.members,
    )
}

/**
 * The source sets a consumer task owns and the ones it only analyses.
 *
 * @property own Every source set the compilation lists directly (KGP `kotlinSourceSets`): for an
 *   Android variant `androidDebug` and `androidMain` (plus flavor and build type sets), for any
 *   other compilation just its default source set.
 * @property analysisOnly The rest of the compilation's sources, `commonMain` and the shared
 *   intermediates (the common fragment).
 */
internal data class MemberSplit(
    val own: List<KotlinSourceSet>,
    val analysisOnly: List<KotlinSourceSet>,
)

/** Splits [compilation]'s source sets into the ones it lists directly and the shared rest. */
internal fun memberSplit(compilation: KotlinCompilation<*>): MemberSplit =
    MemberSplit(
        own = compilation.kotlinSourceSets.toList(),
        analysisOnly = (compilation.allKotlinSourceSets - compilation.kotlinSourceSets).toList(),
    )

/**
 * The Kotlin sources of the source sets [select] picks, read when Gradle resolves the files and
 * empty while [enabled] is false. Never read at registration: KGP can realize the task while it is
 * still creating the Android source sets, before the compilation lists all its members and before
 * any `dependsOn` edge (so `commonMain`) exists. The [Callable] sits directly inside
 * `project.files`, which keeps the task graph configuration-cache safe.
 */
internal fun KotlinCompilation<*>.lazySources(
    enabled: Provider<Boolean>,
    select: (KotlinCompilation<*>) -> Collection<KotlinSourceSet>,
): FileCollection =
    project.files(
        Callable {
            if (enabled.get()) select(this@lazySources).map { it.kotlin } else emptyList<Any>()
        }
    )

/**
 * Appends to [infos] the [extra] source sets whose names are not listed yet, sorted by name. The
 * existing entries keep their order, so a compilation that lists nothing outside its default
 * closure serializes exactly as before.
 */
internal fun appendMissingSourceSets(
    infos: List<SourceSetInfo>,
    extra: List<SourceSetInfo>,
): List<SourceSetInfo> {
    val known = infos.mapTo(hashSetOf()) { it.name }
    return infos + extra.filter { it.name !in known }.distinctBy { it.name }.sortedBy { it.name }
}

/** Every source set [compilation] compiles, with its sorted `dependsOn` names. */
internal fun KotlinCompilation<*>.sourceSetInfos(): List<SourceSetInfo> =
    allKotlinSourceSets.map { set ->
        SourceSetInfo(set.name, set.dependsOn.map { it.name }.sorted())
    }

/**
 * The owner of `commonMain` when it is [SourceSetOwner.Synthetic], or `null` when [targets] do not
 * call for one. This mirrors KGP: no `commonMain` metadata compilation exists when two or more real
 * targets share one non-native platform type, or when the only target is `androidTarget()`.
 * `commonMain` needs no `dependsOn` edges, every main and member source set reaches it.
 */
internal fun predictSyntheticCommonMainOwner(targets: List<TargetNode>): SourceSetOwner.Synthetic? {
    val sets = targets.flatMap { it.mainSourceSets + it.memberSourceSets }
    val parents = sets.associateWith { setOf("commonMain") }
    return assignSourceSetOwners(SourceSetGraph(targets, parents))["commonMain"]
        as? SourceSetOwner.Synthetic
}

/**
 * Whether a compilation of the owner's target is the one that drives the synthetic `commonMain`
 * producer. A `jvm` target drives it from `main`; an `androidjvm` target from the variant whose
 * default source set is the owner's representative (`androidDebug`). No owner, or any other type,
 * means no.
 */
internal fun isSyntheticCommonMainRepresentative(
    owner: SourceSetOwner.Synthetic?,
    compilationName: String,
    defaultSourceSet: String,
    targetType: String,
): Boolean =
    when (targetType) {
        "jvm" -> owner != null && compilationName == "main"
        "androidjvm" -> owner != null && defaultSourceSet == owner.compilationSourceSet
        else -> false
    }

private const val AAR_EXTENSION = "aar"
private const val ANDROID_CLASSES_JAR = "android-classes-jar"

/**
 * The compile dependencies the generation worker can read. KGP's `compileDependencyFiles` for an
 * Android compilation is the raw variant configuration, which holds `.aar` files the K2 driver
 * cannot open, so an `androidx` type in a `@Fake` signature would stay unresolved. For an Android
 * compilation this keeps the jars and directories and adds the unpacked class jars AGP exposes as
 * `android-classes-jar` (a lenient artifact view, so a dependency without one is skipped). Any
 * other compilation gets `compileDependencyFiles` unchanged. Both parts stay lazy and
 * configuration-cache safe: no lambda captures the compilation.
 */
internal fun KotlinCompilation<*>.workerDependencyFiles(): FileCollection {
    if (platformType != KotlinPlatformType.androidJvm) return compileDependencyFiles
    val classJars =
        project.configurations
            .getByName(compileDependencyConfigurationName)
            .incoming
            .artifactView { view ->
                view.isLenient = true
                view.attributes.attribute(
                    Attribute.of(
                        ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE.name,
                        String::class.java,
                    ),
                    ANDROID_CLASSES_JAR,
                )
            }
            .files
    return project.files(compileDependencyFiles.filter { it.extension != AAR_EXTENSION }, classJars)
}
